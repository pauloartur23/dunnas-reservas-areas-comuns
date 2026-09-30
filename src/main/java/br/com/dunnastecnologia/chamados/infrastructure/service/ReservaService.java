package br.com.dunnastecnologia.chamados.infrastructure.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.application.pagination.PageResult;
import br.com.dunnastecnologia.chamados.domain.model.Administrador;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.ReservaHistorico;
import br.com.dunnastecnologia.chamados.domain.model.StatusReserva;
import br.com.dunnastecnologia.chamados.domain.validation.ValidationLimits;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.exception.ResourceNotFoundException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AdministradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaHistoricoRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.InputValidationSupport;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.PageResultMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReservaService implements ReservaUseCases {

    private static final String AUTOR_MORADOR = "MORADOR";
    private static final String AUTOR_ADMINISTRADOR = "ADMINISTRADOR";

    private final AreaComumRepository areaComumRepository;
    private final ReservaRepository reservaRepository;
    private final ReservaHistoricoRepository reservaHistoricoRepository;
    private final MoradorRepository moradorRepository;
    private final AdministradorRepository administradorRepository;
    private final AuthenticatedUserValidator authenticatedUserValidator;

    public ReservaService(
            AreaComumRepository areaComumRepository,
            ReservaRepository reservaRepository,
            ReservaHistoricoRepository reservaHistoricoRepository,
            MoradorRepository moradorRepository,
            AdministradorRepository administradorRepository,
            AuthenticatedUserValidator authenticatedUserValidator
    ) {
        this.areaComumRepository = areaComumRepository;
        this.reservaRepository = reservaRepository;
        this.reservaHistoricoRepository = reservaHistoricoRepository;
        this.moradorRepository = moradorRepository;
        this.administradorRepository = administradorRepository;
        this.authenticatedUserValidator = authenticatedUserValidator;
    }

    // ---------- RF-01: cadastro e manutencao de areas comuns ----------

    @Override
    @Transactional
    public AreaComum cadastrarAreaComum(
            AuthenticatedUser admin,
            String nome,
            String descricao,
            LocalTime horarioAbertura,
            LocalTime horarioFechamento,
            Set<DayOfWeek> diasFuncionamento
    ) {
        authenticatedUserValidator.assertAdministrador(admin);
        String nomeNormalizado = normalizarNomeArea(nome);
        String descricaoNormalizada = normalizarDescricaoArea(descricao);
        validarHorarioFuncionamento(horarioAbertura, horarioFechamento);
        validarNomeUnico(nomeNormalizado, null);

        AreaComum areaComum = new AreaComum();
        areaComum.setNome(nomeNormalizado);
        areaComum.setDescricao(descricaoNormalizada);
        areaComum.setHorarioAbertura(horarioAbertura);
        areaComum.setHorarioFechamento(horarioFechamento);
        areaComum.setDiasFuncionamento(diasFuncionamento != null ? diasFuncionamento : new HashSet<>());
        areaComum.setAtiva(Boolean.TRUE);
        try {
            return areaComumRepository.save(areaComum);
        } catch (DataIntegrityViolationException e) {
            // Segunda camada de protecao: cobre a corrida entre dois cadastros
            // simultaneos com o mesmo nome que passaram pela checagem acima.
            throw new BusinessRuleException("Ja existe uma area comum com este nome");
        }
    }

    @Override
    @Transactional
    public AreaComum atualizarAreaComum(
            AuthenticatedUser admin,
            UUID areaComumId,
            String nome,
            String descricao,
            LocalTime horarioAbertura,
            LocalTime horarioFechamento,
            Set<DayOfWeek> diasFuncionamento
    ) {
        authenticatedUserValidator.assertAdministrador(admin);
        String nomeNormalizado = normalizarNomeArea(nome);
        String descricaoNormalizada = normalizarDescricaoArea(descricao);
        validarHorarioFuncionamento(horarioAbertura, horarioFechamento);
        validarNomeUnico(nomeNormalizado, areaComumId);

        AreaComum areaComum = areaComumRepository.findById(areaComumId)
                .orElseThrow(() -> new ResourceNotFoundException("Area comum nao encontrada"));

        // RN-01-01: a alteracao vale para novas solicitacoes; reservas existentes nao sao tocadas.
        areaComum.setNome(nomeNormalizado);
        areaComum.setDescricao(descricaoNormalizada);
        areaComum.setHorarioAbertura(horarioAbertura);
        areaComum.setHorarioFechamento(horarioFechamento);
        areaComum.setDiasFuncionamento(diasFuncionamento != null ? diasFuncionamento : new HashSet<>());
        try {
            return areaComumRepository.save(areaComum);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleException("Ja existe uma area comum com este nome");
        }
    }

    @Override
    public AreaComum buscarAreaComumPorId(AuthenticatedUser admin, UUID areaComumId) {
        authenticatedUserValidator.assertAdministrador(admin);
        return areaComumRepository.findById(areaComumId)
                .orElseThrow(() -> new ResourceNotFoundException("Area comum nao encontrada"));
    }

    @Override
    @Transactional
    public AreaComum alterarDisponibilidadeAreaComum(AuthenticatedUser admin, UUID areaComumId, boolean ativa) {
        authenticatedUserValidator.assertAdministrador(admin);
        AreaComum areaComum = areaComumRepository.findById(areaComumId)
                .orElseThrow(() -> new ResourceNotFoundException("Area comum nao encontrada"));

        // RN-01-01: apenas alterna o flag; reservas existentes nunca sao tocadas aqui.
        areaComum.setAtiva(ativa);
        return areaComumRepository.save(areaComum);
    }

    @Override
    public List<AreaComum> listarAreasComuns() {
        return areaComumRepository.findAll();
    }

    // ---------- RF-02: disponibilidade e solicitacao ----------

    @Override
    public boolean consultarDisponibilidade(
            AuthenticatedUser morador, UUID areaComumId, LocalDate data, LocalTime horaInicio, LocalTime horaFim
    ) {
        authenticatedUserValidator.assertMorador(morador);
        AreaComum areaComum = areaComumRepository.findById(areaComumId)
                .orElseThrow(() -> new ResourceNotFoundException("Area comum nao encontrada"));
        validarIntervalo(data, horaInicio, horaFim, false);
        validarJanelaDeFuncionamento(areaComum, horaInicio, horaFim);
        validarDiaDeFuncionamento(areaComum, data);

        // RN-01-05: so reservas APROVADA contam como ocupado.
        return !reservaRepository.existeReservaAprovadaConflitante(areaComumId, data, horaInicio, horaFim);
    }

    @Override
    @Transactional
    public Reserva solicitarReserva(
            AuthenticatedUser morador, UUID areaComumId, LocalDate data, LocalTime horaInicio, LocalTime horaFim
    ) {
        authenticatedUserValidator.assertMorador(morador);
        validarIntervalo(data, horaInicio, horaFim, true);

        AreaComum areaComum = areaComumRepository.findById(areaComumId)
                .orElseThrow(() -> new ResourceNotFoundException("Area comum nao encontrada"));
        if (!Boolean.TRUE.equals(areaComum.getAtiva())) {
            // RN-01-01: area retirada/desativada nao aceita novas solicitacoes.
            throw new BusinessRuleException("Area comum nao esta disponivel para novas solicitacoes");
        }
        validarJanelaDeFuncionamento(areaComum, horaInicio, horaFim);
        validarDiaDeFuncionamento(areaComum, data);

        Morador moradorEntity = moradorRepository.findByIdAndAtivoTrue(morador.id())
                .orElseThrow(() -> new ResourceNotFoundException("Morador nao encontrado"));

        Reserva reserva = new Reserva();
        reserva.setAreaComum(areaComum);
        reserva.setMorador(moradorEntity);
        reserva.setData(data);
        reserva.setHoraInicio(horaInicio);
        reserva.setHoraFim(horaFim);
        reserva.setStatus(StatusReserva.SOLICITADA); // RN-01-04
        reserva.setDataSolicitacao(LocalDateTime.now());
        Reserva reservaSalva = reservaRepository.save(reserva);
        registrarHistorico(reservaSalva, null, StatusReserva.SOLICITADA, AUTOR_MORADOR, moradorEntity.getNome(), null);
        return reservaSalva;
    }

    // ---------- RF-05: consulta e acompanhamento ----------

    @Override
    public PageResult<Reserva> listarMinhasReservas(
            AuthenticatedUser morador, String status, UUID areaComumId, LocalDate data, PageRequest pageRequest
    ) {
        authenticatedUserValidator.assertMorador(morador);
        return PageResultMapper.fromPage(
                reservaRepository.buscarParaMorador(morador.id(), status, areaComumId, data, pageRequest)
        );
    }

    @Override
    public PageResult<Reserva> listarReservasParaAdmin(
            AuthenticatedUser admin, String status, UUID areaComumId, LocalDate data, PageRequest pageRequest
    ) {
        authenticatedUserValidator.assertAdministrador(admin);
        return PageResultMapper.fromPage(
                reservaRepository.buscarParaAdmin(status, areaComumId, data, pageRequest)
        );
    }

    @Override
    public Reserva buscarMinhaReserva(AuthenticatedUser morador, UUID reservaId) {
        authenticatedUserValidator.assertMorador(morador);
        // RNF-03: reserva de outro morador e reserva inexistente recebem a mesma resposta.
        if (!reservaRepository.existsByIdAndMoradorId(reservaId, morador.id())) {
            throw new ResourceNotFoundException("Reserva nao encontrada para o morador");
        }
        return reservaRepository.findByIdAndMoradorId(morador.id(), reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva nao encontrada para o morador"));
    }

    @Override
    public List<ReservaHistorico> listarHistoricoDaMinhaReserva(AuthenticatedUser morador, UUID reservaId) {
        authenticatedUserValidator.assertMorador(morador);
        // RNF-03: mesma checagem de posse usada em buscarMinhaReserva.
        if (!reservaRepository.existsByIdAndMoradorId(reservaId, morador.id())) {
            throw new ResourceNotFoundException("Reserva nao encontrada para o morador");
        }
        return reservaHistoricoRepository.findByReservaIdOrderByDataEventoAsc(reservaId);
    }

    // ---------- RF-04: aprovacao e negacao ----------

    @Override
    @Transactional
    public Reserva aprovarReserva(AuthenticatedUser admin, UUID reservaId) {
        authenticatedUserValidator.assertAdministrador(admin);

        Reserva reserva = carregarReservaComLockDaArea(reservaId);
        assertDecisaoPermitida(reserva);
        if (jaComecou(reserva)) {
            // CA-01-12: reserva cujo inicio ja ocorreu nao pode mais ser aprovada.
            throw new BusinessRuleException("Nao e possivel aprovar uma reserva cujo horario inicial ja comecou");
        }

        boolean conflita = reservaRepository.existeReservaAprovadaConflitante(
                reserva.getAreaComum().getId(), reserva.getData(), reserva.getHoraInicio(), reserva.getHoraFim()
        );
        if (conflita) {
            // RN-01-07: nao aprova se, no momento da decisao, ja existe aprovada conflitante.
            throw new BusinessRuleException("Ja existe uma reserva aprovada conflitante para esta area e horario");
        }

        Administrador administradorEntity = administradorRepository.findByIdAndAtivoTrue(admin.id())
                .orElseThrow(() -> new ResourceNotFoundException("Administrador nao encontrado"));

        StatusReserva statusAnterior = reserva.getStatus();
        reserva.setStatus(StatusReserva.APROVADA);
        reserva.setAdministradorDecisao(administradorEntity);
        reserva.setDataDecisao(LocalDateTime.now());
        Reserva reservaSalva = reservaRepository.save(reserva);
        registrarHistorico(reservaSalva, statusAnterior, StatusReserva.APROVADA, AUTOR_ADMINISTRADOR, administradorEntity.getNome(), null);
        return reservaSalva;
    }

    @Override
    @Transactional
    public Reserva negarReserva(AuthenticatedUser admin, UUID reservaId, String motivo) {
        authenticatedUserValidator.assertAdministrador(admin);
        String motivoNormalizado = InputValidationSupport.normalizeRequiredText(
                motivo,
                "Motivo da negacao e obrigatorio",
                "Motivo da negacao deve ter no maximo " + ValidationLimits.DEFAULT_TEXT_MAX_LENGTH + " caracteres",
                ValidationLimits.DEFAULT_TEXT_MAX_LENGTH
        ); // RN-01-08: motivo nao vazio

        Reserva reserva = carregarReservaComLockDaArea(reservaId);
        assertDecisaoPermitida(reserva);

        Administrador administradorEntity = administradorRepository.findByIdAndAtivoTrue(admin.id())
                .orElseThrow(() -> new ResourceNotFoundException("Administrador nao encontrado"));

        StatusReserva statusAnterior = reserva.getStatus();
        reserva.setStatus(StatusReserva.NEGADA);
        reserva.setMotivoNegacao(motivoNormalizado);
        reserva.setAdministradorDecisao(administradorEntity);
        reserva.setDataDecisao(LocalDateTime.now());
        Reserva reservaSalva = reservaRepository.save(reserva);
        registrarHistorico(reservaSalva, statusAnterior, StatusReserva.NEGADA, AUTOR_ADMINISTRADOR, administradorEntity.getNome(), motivoNormalizado);
        return reservaSalva;
    }

    // ---------- RF-06: cancelamento ----------

    @Override
    @Transactional
    public Reserva cancelarComoMorador(AuthenticatedUser morador, UUID reservaId) {
        authenticatedUserValidator.assertMorador(morador);
        if (!reservaRepository.existsByIdAndMoradorId(reservaId, morador.id())) {
            throw new ResourceNotFoundException("Reserva nao encontrada para o morador");
        }
        Reserva reserva = carregarReservaComLockDaArea(reservaId); // RN-01-11
        String autorNome = reserva.getMorador() == null ? null : reserva.getMorador().getNome();
        return cancelar(reserva, AUTOR_MORADOR, autorNome);
    }

    @Override
    @Transactional
    public Reserva cancelarComoAdmin(AuthenticatedUser admin, UUID reservaId) {
        authenticatedUserValidator.assertAdministrador(admin);
        Administrador administradorEntity = administradorRepository.findByIdAndAtivoTrue(admin.id())
                .orElseThrow(() -> new ResourceNotFoundException("Administrador nao encontrado"));
        Reserva reserva = carregarReservaComLockDaArea(reservaId); // RN-01-12
        return cancelar(reserva, AUTOR_ADMINISTRADOR, administradorEntity.getNome());
    }

    private Reserva cancelar(Reserva reserva, String autorTipo, String autorNome) {
        if (reserva.getStatus() != StatusReserva.SOLICITADA && reserva.getStatus() != StatusReserva.APROVADA) {
            // RN-01-14: estados terminais nao podem ser cancelados de novo.
            throw new BusinessRuleException("Somente reservas solicitadas ou aprovadas podem ser canceladas");
        }
        if (jaComecou(reserva)) {
            // RN-01-11/RN-01-12/RN-01-14: horario inicial ja alcancado impede cancelamento.
            throw new BusinessRuleException("Nao e possivel cancelar uma reserva cujo horario inicial ja comecou");
        }

        StatusReserva statusAnterior = reserva.getStatus();
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setDataCancelamento(LocalDateTime.now());
        Reserva reservaSalva = reservaRepository.save(reserva); // RN-01-13: preserva historico, libera disponibilidade
        registrarHistorico(reservaSalva, statusAnterior, StatusReserva.CANCELADA, autorTipo, autorNome, null);
        return reservaSalva;
    }

    // ---------- concorrencia ----------

    /**
     * Toda mudanca de estado de reserva passa por aqui: descobre a area, tranca a area
     * (serializando decisoes concorrentes sobre ela) e so entao le a reserva, garantindo
     * que o estado lido e o mais recente e nao um dado velho de antes da trava.
     */
    private Reserva carregarReservaComLockDaArea(UUID reservaId) {
        UUID areaComumId = reservaRepository.buscarAreaComumIdPorReservaId(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva nao encontrada"));
        areaComumRepository.buscarComLockParaDecisao(areaComumId)
                .orElseThrow(() -> new ResourceNotFoundException("Area comum nao encontrada"));
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva nao encontrada"));
    }

    // ---------- historico (registro de auditoria) ----------

    /**
     * Grava um evento imutavel de mudanca de status. statusAnterior e null apenas
     * na criacao da reserva (nao houve status anterior). Chamado sempre depois do
     * save() da propria reserva, pra garantir que reserva.getId() ja existe.
     */
    private void registrarHistorico(
            Reserva reserva,
            StatusReserva statusAnterior,
            StatusReserva statusNovo,
            String autorTipo,
            String autorNome,
            String observacao
    ) {
        ReservaHistorico historico = new ReservaHistorico();
        historico.setReserva(reserva);
        historico.setStatusAnterior(statusAnterior);
        historico.setStatusNovo(statusNovo);
        historico.setAutorTipo(autorTipo);
        historico.setAutorNome(autorNome);
        historico.setObservacao(observacao);
        historico.setDataEvento(LocalDateTime.now());
        reservaHistoricoRepository.save(historico);
    }

    // ---------- validacoes privadas compartilhadas ----------

    private void assertDecisaoPermitida(Reserva reserva) {
        if (reserva.getStatus() != StatusReserva.SOLICITADA) {
            // RN-01-14: reserva ja decidida ou cancelada nao pode ser reaprovada/renegada.
            throw new BusinessRuleException("Somente reservas solicitadas podem ser aprovadas ou negadas");
        }
    }

    private boolean jaComecou(Reserva reserva) {
        // RN-01-15: referencia temporal unica (relogio da JVM, timezone America/Sao_Paulo)
        LocalDateTime inicio = LocalDateTime.of(reserva.getData(), reserva.getHoraInicio());
        return !inicio.isAfter(LocalDateTime.now());
    }

    private void validarIntervalo(LocalDate data, LocalTime horaInicio, LocalTime horaFim, boolean exigirInicioFuturo) {
        if (data == null || horaInicio == null || horaFim == null) {
            throw new BusinessRuleException("Data, horario de inicio e horario de fim sao obrigatorios");
        }
        if (!horaFim.isAfter(horaInicio)) {
            // RN-01-02
            throw new BusinessRuleException("Horario de fim deve ser posterior ao horario de inicio");
        }
        if (exigirInicioFuturo) {
            // RN-01-02 + RN-01-15
            LocalDateTime inicio = LocalDateTime.of(data, horaInicio);
            if (!inicio.isAfter(LocalDateTime.now())) {
                throw new BusinessRuleException("O horario inicial da reserva deve ser futuro");
            }
        }
    }

    private void validarJanelaDeFuncionamento(AreaComum areaComum, LocalTime horaInicio, LocalTime horaFim) {
        LocalTime abertura = areaComum.getHorarioAbertura();
        LocalTime fechamento = areaComum.getHorarioFechamento();
        if (abertura == null || fechamento == null) {
            return; // area sem horario de funcionamento nao tem restricao
        }
        if (horaInicio.isBefore(abertura) || horaFim.isAfter(fechamento)) {
            throw new BusinessRuleException(
                    "A area so funciona das " + abertura + " as " + fechamento
            );
        }
    }

    private void validarHorarioFuncionamento(LocalTime abertura, LocalTime fechamento) {
        if (abertura == null && fechamento == null) {
            return;
        }
        if (abertura == null || fechamento == null) {
            throw new BusinessRuleException(
                    "Informe o horario de abertura e o de fechamento, ou deixe os dois vazios"
            );
        }
        if (!fechamento.isAfter(abertura)) {
            throw new BusinessRuleException("Horario de fechamento deve ser posterior ao de abertura");
        }
    }

    private void validarDiaDeFuncionamento(AreaComum areaComum, LocalDate data) {
        Set<DayOfWeek> dias = areaComum.getDiasFuncionamento();
        if (dias == null || dias.isEmpty()) {
            return; // sem dias cadastrados: area funciona todos os dias
        }
        if (!dias.contains(data.getDayOfWeek())) {
            throw new BusinessRuleException(
                    "Esta area nao funciona aos " + nomeDiaSemana(data.getDayOfWeek())
            );
        }
    }

    /**
     * RN adicionada por nos: nome de area comum e unico ignorando maiuscula/minuscula.
     * areaComumIdIgnorado e null no cadastro e o id da propria area na edicao (pra nao
     * acusar a area colidindo com o proprio nome dela mesma).
     */
    private void validarNomeUnico(String nomeNormalizado, UUID areaComumIdIgnorado) {
        boolean duplicado = areaComumIdIgnorado == null
                ? areaComumRepository.existsByNomeIgnoreCase(nomeNormalizado)
                : areaComumRepository.existsByNomeIgnoreCaseAndIdNot(nomeNormalizado, areaComumIdIgnorado);
        if (duplicado) {
            throw new BusinessRuleException("Ja existe uma area comum com este nome");
        }
    }

    private String nomeDiaSemana(DayOfWeek diaSemana) {
        return diaSemana.getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
    }

    private String normalizarNomeArea(String nome) {
        return InputValidationSupport.normalizeRequiredText(
                nome,
                "Nome da area comum e obrigatorio",
                "Nome da area comum deve ter no maximo " + ValidationLimits.DEFAULT_TEXT_MAX_LENGTH + " caracteres",
                ValidationLimits.DEFAULT_TEXT_MAX_LENGTH
        );
    }

    private String normalizarDescricaoArea(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return null;
        }
        String normalizada = descricao.trim();
        if (normalizada.length() > ValidationLimits.DEFAULT_TEXT_MAX_LENGTH) {
            throw new BusinessRuleException(
                    "Descricao da area comum deve ter no maximo " + ValidationLimits.DEFAULT_TEXT_MAX_LENGTH + " caracteres"
            );
        }
        return normalizada;
    }
}