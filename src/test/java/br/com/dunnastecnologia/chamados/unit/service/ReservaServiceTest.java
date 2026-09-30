package br.com.dunnastecnologia.chamados.infrastructure.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.Administrador;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.ReservaHistorico;
import br.com.dunnastecnologia.chamados.domain.model.StatusReserva;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.exception.ResourceNotFoundException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AdministradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaHistoricoRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private AreaComumRepository areaComumRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ReservaHistoricoRepository reservaHistoricoRepository;
    @Mock
    private MoradorRepository moradorRepository;
    @Mock
    private AdministradorRepository administradorRepository;
    @Mock
    private AuthenticatedUserValidator authenticatedUserValidator;

    @InjectMocks
    private ReservaService reservaService;

    private static final LocalDate AMANHA = LocalDate.now().plusDays(1);

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");
    }

    private AuthenticatedUser morador() {
        return new AuthenticatedUser(UUID.randomUUID(), "morador@cond.local", "ROLE_MORADOR");
    }

    // ---------- cadastro/atualizacao de area comum (nome unico) ----------

    @Test
    void cadastrarAreaComumDeveCriarQuandoNomeNaoExiste() {
        AuthenticatedUser admin = admin();
        when(areaComumRepository.existsByNomeIgnoreCase("Piscina")).thenReturn(false);
        when(areaComumRepository.save(any(AreaComum.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AreaComum areaComum = reservaService.cadastrarAreaComum(
                admin, "Piscina", "Area de lazer", LocalTime.of(7, 0), LocalTime.of(20, 0), Set.of(DayOfWeek.MONDAY)
        );

        assertEquals("Piscina", areaComum.getNome());
        assertTrue(areaComum.getAtiva());
    }

    @Test
    void cadastrarAreaComumDeveFalharQuandoJaExisteNomeIgualIgnorandoCaixa() {
        AuthenticatedUser admin = admin();
        when(areaComumRepository.existsByNomeIgnoreCase("Piscina")).thenReturn(true);

        assertThrows(
                BusinessRuleException.class,
                () -> reservaService.cadastrarAreaComum(admin, "Piscina", null, null, null, null)
        );

        verify(areaComumRepository, never()).save(any());
    }

    @Test
    void cadastrarAreaComumDeveFalharQuandoHorarioDeFechamentoNaoForPosteriorAoDeAbertura() {
        AuthenticatedUser admin = admin();

        assertThrows(
                BusinessRuleException.class,
                () -> reservaService.cadastrarAreaComum(admin, "Salao", null, LocalTime.of(10, 0), LocalTime.of(9, 0), null)
        );

        verify(areaComumRepository, never()).save(any());
    }

    @Test
    void atualizarAreaComumDeveFalharQuandoNomeDuplicadoIgnorandoAPropriaArea() {
        AuthenticatedUser admin = admin();
        UUID areaId = UUID.randomUUID();
        when(areaComumRepository.existsByNomeIgnoreCaseAndIdNot("Piscina Aberta", areaId)).thenReturn(true);

        assertThrows(
                BusinessRuleException.class,
                () -> reservaService.atualizarAreaComum(admin, areaId, "Piscina Aberta", null, null, null, null)
        );

        verify(areaComumRepository, never()).findById(any());
    }

    @Test
    void atualizarAreaComumDeveFalharQuandoAreaNaoEncontrada() {
        AuthenticatedUser admin = admin();
        UUID areaId = UUID.randomUUID();
        when(areaComumRepository.existsByNomeIgnoreCaseAndIdNot("Piscina", areaId)).thenReturn(false);
        when(areaComumRepository.findById(areaId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> reservaService.atualizarAreaComum(admin, areaId, "Piscina", null, null, null, null)
        );
    }

    // ---------- consulta de disponibilidade ----------

    @Test
    void consultarDisponibilidadeDeveRetornarTrueQuandoNaoHaReservaAprovadaConflitante() {
        AuthenticatedUser morador = morador();
        UUID areaId = UUID.randomUUID();
        AreaComum area = new AreaComum();
        area.setId(areaId);

        when(areaComumRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.existeReservaAprovadaConflitante(areaId, AMANHA, LocalTime.of(10, 0), LocalTime.of(11, 0)))
                .thenReturn(false);

        boolean disponivel = reservaService.consultarDisponibilidade(
                morador, areaId, AMANHA, LocalTime.of(10, 0), LocalTime.of(11, 0)
        );

        assertTrue(disponivel);
    }

    @Test
    void consultarDisponibilidadeDeveRetornarFalseQuandoHaReservaAprovadaConflitante() {
        AuthenticatedUser morador = morador();
        UUID areaId = UUID.randomUUID();
        AreaComum area = new AreaComum();
        area.setId(areaId);

        when(areaComumRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.existeReservaAprovadaConflitante(areaId, AMANHA, LocalTime.of(10, 0), LocalTime.of(11, 0)))
                .thenReturn(true);

        boolean disponivel = reservaService.consultarDisponibilidade(
                morador, areaId, AMANHA, LocalTime.of(10, 0), LocalTime.of(11, 0)
        );

        assertFalse(disponivel);
    }

    @Test
    void consultarDisponibilidadeDeveFalharQuandoForaDaJanelaDeFuncionamento() {
        AuthenticatedUser morador = morador();
        UUID areaId = UUID.randomUUID();
        AreaComum area = new AreaComum();
        area.setId(areaId);
        area.setHorarioAbertura(LocalTime.of(8, 0));
        area.setHorarioFechamento(LocalTime.of(18, 0));

        when(areaComumRepository.findById(areaId)).thenReturn(Optional.of(area));

        assertThrows(
                BusinessRuleException.class,
                () -> reservaService.consultarDisponibilidade(morador, areaId, AMANHA, LocalTime.of(19, 0), LocalTime.of(20, 0))
        );
    }

    @Test
    void consultarDisponibilidadeDeveFalharQuandoDiaNaoEstaEntreOsDiasDeFuncionamento() {
        AuthenticatedUser morador = morador();
        UUID areaId = UUID.randomUUID();
        AreaComum area = new AreaComum();
        area.setId(areaId);
        area.setDiasFuncionamento(new HashSet<>(Set.of(DayOfWeek.MONDAY)));

        LocalDate domingo = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));

        when(areaComumRepository.findById(areaId)).thenReturn(Optional.of(area));

        assertThrows(
                BusinessRuleException.class,
                () -> reservaService.consultarDisponibilidade(morador, areaId, domingo, LocalTime.of(10, 0), LocalTime.of(11, 0))
        );
    }

    // ---------- solicitacao de reserva ----------

    @Test
    void solicitarReservaDeveCriarComStatusSolicitadaERegistrarHistorico() {
        AuthenticatedUser morador = morador();
        UUID areaId = UUID.randomUUID();
        AreaComum area = new AreaComum();
        area.setId(areaId);
        area.setAtiva(Boolean.TRUE);

        Morador moradorEntity = new Morador();
        moradorEntity.setId(morador.id());
        moradorEntity.setNome("Paulo Pedro");

        when(areaComumRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(moradorRepository.findByIdAndAtivoTrue(morador.id())).thenReturn(Optional.of(moradorEntity));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva reserva = reservaService.solicitarReserva(morador, areaId, AMANHA, LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertEquals(StatusReserva.SOLICITADA, reserva.getStatus());
        assertEquals(moradorEntity, reserva.getMorador());

        ArgumentCaptor<ReservaHistorico> captor = ArgumentCaptor.forClass(ReservaHistorico.class);
        verify(reservaHistoricoRepository).save(captor.capture());
        ReservaHistorico historico = captor.getValue();
        assertNull(historico.getStatusAnterior());
        assertEquals(StatusReserva.SOLICITADA, historico.getStatusNovo());
        assertEquals("MORADOR", historico.getAutorTipo());
        assertEquals("Paulo Pedro", historico.getAutorNome());
    }

    @Test
    void solicitarReservaDeveFalharQuandoAreaComumEstaInativa() {
        AuthenticatedUser morador = morador();
        UUID areaId = UUID.randomUUID();
        AreaComum area = new AreaComum();
        area.setId(areaId);
        area.setAtiva(Boolean.FALSE);

        when(areaComumRepository.findById(areaId)).thenReturn(Optional.of(area));

        assertThrows(
                BusinessRuleException.class,
                () -> reservaService.solicitarReserva(morador, areaId, AMANHA, LocalTime.of(10, 0), LocalTime.of(11, 0))
        );

        verify(reservaRepository, never()).save(any());
        verify(reservaHistoricoRepository, never()).save(any());
    }

    // ---------- aprovacao ----------

    @Test
    void aprovarReservaDeveAprovarERegistrarHistoricoQuandoNaoHaConflito() {
        AuthenticatedUser admin = admin();
        UUID reservaId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();

        AreaComum area = new AreaComum();
        area.setId(areaId);

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setAreaComum(area);
        reserva.setStatus(StatusReserva.SOLICITADA);
        reserva.setData(AMANHA);
        reserva.setHoraInicio(LocalTime.of(10, 0));
        reserva.setHoraFim(LocalTime.of(11, 0));

        Administrador administradorEntity = new Administrador();
        administradorEntity.setId(admin.id());
        administradorEntity.setNome("Admin Um");

        when(reservaRepository.buscarAreaComumIdPorReservaId(reservaId)).thenReturn(Optional.of(areaId));
        when(areaComumRepository.buscarComLockParaDecisao(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(reservaRepository.existeReservaAprovadaConflitante(areaId, AMANHA, LocalTime.of(10, 0), LocalTime.of(11, 0)))
                .thenReturn(false);
        when(administradorRepository.findByIdAndAtivoTrue(admin.id())).thenReturn(Optional.of(administradorEntity));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva aprovada = reservaService.aprovarReserva(admin, reservaId);

        assertEquals(StatusReserva.APROVADA, aprovada.getStatus());
        assertEquals(administradorEntity, aprovada.getAdministradorDecisao());

        ArgumentCaptor<ReservaHistorico> captor = ArgumentCaptor.forClass(ReservaHistorico.class);
        verify(reservaHistoricoRepository).save(captor.capture());
        ReservaHistorico historico = captor.getValue();
        assertEquals(StatusReserva.SOLICITADA, historico.getStatusAnterior());
        assertEquals(StatusReserva.APROVADA, historico.getStatusNovo());
        assertEquals("ADMINISTRADOR", historico.getAutorTipo());
    }

    /**
     * Este teste prova a REGRA (deteccao de conflito) a nivel de service.
     * A prova de que a TRAVA (lock pessimista) realmente serializa duas aprovacoes
     * concorrentes e feita separadamente, em ReservaConcorrenciaIntegrationTest,
     * com banco real e duas threads de verdade — um mock nao consegue provar isso.
     */
    @Test
    void aprovarReservaDeveFalharQuandoJaExisteReservaAprovadaConflitante() {
        AuthenticatedUser admin = admin();
        UUID reservaId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();

        AreaComum area = new AreaComum();
        area.setId(areaId);

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setAreaComum(area);
        reserva.setStatus(StatusReserva.SOLICITADA);
        reserva.setData(AMANHA);
        reserva.setHoraInicio(LocalTime.of(10, 0));
        reserva.setHoraFim(LocalTime.of(11, 0));

        when(reservaRepository.buscarAreaComumIdPorReservaId(reservaId)).thenReturn(Optional.of(areaId));
        when(areaComumRepository.buscarComLockParaDecisao(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(reservaRepository.existeReservaAprovadaConflitante(areaId, AMANHA, LocalTime.of(10, 0), LocalTime.of(11, 0)))
                .thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> reservaService.aprovarReserva(admin, reservaId));

        verify(reservaRepository, never()).save(any());
        verify(reservaHistoricoRepository, never()).save(any());
    }

    @Test
    void aprovarReservaDeveFalharQuandoReservaJaComecou() {
        AuthenticatedUser admin = admin();
        UUID reservaId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();

        AreaComum area = new AreaComum();
        area.setId(areaId);

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setAreaComum(area);
        reserva.setStatus(StatusReserva.SOLICITADA);
        reserva.setData(LocalDate.now());
        reserva.setHoraInicio(LocalTime.now().minusMinutes(30));
        reserva.setHoraFim(LocalTime.now().plusHours(1));

        when(reservaRepository.buscarAreaComumIdPorReservaId(reservaId)).thenReturn(Optional.of(areaId));
        when(areaComumRepository.buscarComLockParaDecisao(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));

        assertThrows(BusinessRuleException.class, () -> reservaService.aprovarReserva(admin, reservaId));

        verify(reservaRepository, never()).save(any());
    }

    // ---------- negacao ----------

    @Test
    void negarReservaDeveNegarComMotivoERegistrarHistorico() {
        AuthenticatedUser admin = admin();
        UUID reservaId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        String motivo = "Manutencao programada";

        AreaComum area = new AreaComum();
        area.setId(areaId);

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setAreaComum(area);
        reserva.setStatus(StatusReserva.SOLICITADA);
        reserva.setData(AMANHA);
        reserva.setHoraInicio(LocalTime.of(10, 0));
        reserva.setHoraFim(LocalTime.of(11, 0));

        Administrador administradorEntity = new Administrador();
        administradorEntity.setId(admin.id());
        administradorEntity.setNome("Admin Um");

        when(reservaRepository.buscarAreaComumIdPorReservaId(reservaId)).thenReturn(Optional.of(areaId));
        when(areaComumRepository.buscarComLockParaDecisao(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(administradorRepository.findByIdAndAtivoTrue(admin.id())).thenReturn(Optional.of(administradorEntity));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva negada = reservaService.negarReserva(admin, reservaId, motivo);

        assertEquals(StatusReserva.NEGADA, negada.getStatus());
        assertEquals(motivo, negada.getMotivoNegacao());

        ArgumentCaptor<ReservaHistorico> captor = ArgumentCaptor.forClass(ReservaHistorico.class);
        verify(reservaHistoricoRepository).save(captor.capture());
        assertEquals(motivo, captor.getValue().getObservacao());
        assertEquals(StatusReserva.NEGADA, captor.getValue().getStatusNovo());
    }

    @Test
    void negarReservaDeveFalharQuandoMotivoEstaVazio() {
        AuthenticatedUser admin = admin();
        UUID reservaId = UUID.randomUUID();

        assertThrows(BusinessRuleException.class, () -> reservaService.negarReserva(admin, reservaId, "   "));

        verify(reservaRepository, never()).buscarAreaComumIdPorReservaId(any());
    }

    // ---------- cancelamento ----------

    @Test
    void cancelarComoMoradorDeveCancelarERegistrarHistorico() {
        AuthenticatedUser morador = morador();
        UUID reservaId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();

        AreaComum area = new AreaComum();
        area.setId(areaId);

        Morador moradorEntity = new Morador();
        moradorEntity.setId(morador.id());
        moradorEntity.setNome("Paulo Pedro");

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setAreaComum(area);
        reserva.setMorador(moradorEntity);
        reserva.setStatus(StatusReserva.APROVADA);
        reserva.setData(AMANHA);
        reserva.setHoraInicio(LocalTime.of(10, 0));
        reserva.setHoraFim(LocalTime.of(11, 0));

        when(reservaRepository.existsByIdAndMoradorId(reservaId, morador.id())).thenReturn(true);
        when(reservaRepository.buscarAreaComumIdPorReservaId(reservaId)).thenReturn(Optional.of(areaId));
        when(areaComumRepository.buscarComLockParaDecisao(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva cancelada = reservaService.cancelarComoMorador(morador, reservaId);

        assertEquals(StatusReserva.CANCELADA, cancelada.getStatus());

        ArgumentCaptor<ReservaHistorico> captor = ArgumentCaptor.forClass(ReservaHistorico.class);
        verify(reservaHistoricoRepository).save(captor.capture());
        assertEquals("MORADOR", captor.getValue().getAutorTipo());
        assertEquals("Paulo Pedro", captor.getValue().getAutorNome());
    }

    @Test
    void cancelarComoAdminDeveCancelarERegistrarHistorico() {
        AuthenticatedUser admin = admin();
        UUID reservaId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();

        AreaComum area = new AreaComum();
        area.setId(areaId);

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setAreaComum(area);
        reserva.setStatus(StatusReserva.SOLICITADA);
        reserva.setData(AMANHA);
        reserva.setHoraInicio(LocalTime.of(10, 0));
        reserva.setHoraFim(LocalTime.of(11, 0));

        Administrador administradorEntity = new Administrador();
        administradorEntity.setId(admin.id());
        administradorEntity.setNome("Admin Um");

        when(administradorRepository.findByIdAndAtivoTrue(admin.id())).thenReturn(Optional.of(administradorEntity));
        when(reservaRepository.buscarAreaComumIdPorReservaId(reservaId)).thenReturn(Optional.of(areaId));
        when(areaComumRepository.buscarComLockParaDecisao(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva cancelada = reservaService.cancelarComoAdmin(admin, reservaId);

        assertEquals(StatusReserva.CANCELADA, cancelada.getStatus());

        ArgumentCaptor<ReservaHistorico> captor = ArgumentCaptor.forClass(ReservaHistorico.class);
        verify(reservaHistoricoRepository).save(captor.capture());
        assertEquals("ADMINISTRADOR", captor.getValue().getAutorTipo());
        assertEquals("Admin Um", captor.getValue().getAutorNome());
    }

    @Test
    void cancelarDeveFalharQuandoReservaJaEstaCancelada() {
        AuthenticatedUser morador = morador();
        UUID reservaId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();

        AreaComum area = new AreaComum();
        area.setId(areaId);

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setAreaComum(area);
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setData(AMANHA);
        reserva.setHoraInicio(LocalTime.of(10, 0));
        reserva.setHoraFim(LocalTime.of(11, 0));

        when(reservaRepository.existsByIdAndMoradorId(reservaId, morador.id())).thenReturn(true);
        when(reservaRepository.buscarAreaComumIdPorReservaId(reservaId)).thenReturn(Optional.of(areaId));
        when(areaComumRepository.buscarComLockParaDecisao(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));

        assertThrows(BusinessRuleException.class, () -> reservaService.cancelarComoMorador(morador, reservaId));

        verify(reservaRepository, never()).save(any());
    }
}