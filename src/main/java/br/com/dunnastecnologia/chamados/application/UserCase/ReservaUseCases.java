package br.com.dunnastecnologia.chamados.application.UserCase;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.application.pagination.PageResult;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ReservaUseCases {

    /**
     * Permite que o administrador cadastre uma nova area comum disponivel para reserva.
     */
    AreaComum cadastrarAreaComum(AuthenticatedUser admin, String nome, String descricao);

    /**
     * Permite que o administrador ative ou desative uma area comum, sem apagar
     * reservas ja existentes vinculadas a ela.
     */
    AreaComum alterarDisponibilidadeAreaComum(AuthenticatedUser admin, UUID areaComumId, boolean ativa);

    /**
     * Lista as areas comuns cadastradas, para consulta de disponibilidade pelo morador
     * ou manutencao pelo administrador.
     */
    List<AreaComum> listarAreasComuns();

    /**
     * Permite que um morador ativo verifique se uma area esta disponivel
     * para um intervalo de data e horario, antes de solicitar a reserva.
     */
    boolean consultarDisponibilidade(AuthenticatedUser morador, UUID areaComumId, LocalDate data, LocalTime horaInicio, LocalTime horaFim);

    /**
     * Permite que um morador ativo solicite a reserva de uma area comum,
     * criando-a inicialmente no estado SOLICITADA.
     */
    Reserva solicitarReserva(AuthenticatedUser morador, UUID areaComumId, LocalDate data, LocalTime horaInicio, LocalTime horaFim);

    /**
     * Lista apenas as reservas do proprio morador, respeitando a regra de que
     * ele nao acessa reservas de outros moradores.
     */
    PageResult<Reserva> listarMinhasReservas(AuthenticatedUser morador, String status, UUID areaComumId, LocalDate data, PageRequest pageRequest);

    /**
     * Lista todas as reservas do condominio, para acompanhamento administrativo.
     */
    PageResult<Reserva> listarReservasParaAdmin(AuthenticatedUser admin, String status, UUID areaComumId, LocalDate data, PageRequest pageRequest);

    /**
     * Recupera uma reserva especifica do morador autenticado, sem expor
     * reservas de outras pessoas.
     */
    Reserva buscarMinhaReserva(AuthenticatedUser morador, UUID reservaId);

    /**
     * Permite que o administrador aprove uma solicitacao sem conflito no momento
     * da decisao, tornando-a a unica reserva aprovada daquele intervalo na area.
     */
    Reserva aprovarReserva(AuthenticatedUser admin, UUID reservaId);

    /**
     * Permite que o administrador negue uma solicitacao, exigindo um motivo
     * nao vazio que fica preservado no historico.
     */
    Reserva negarReserva(AuthenticatedUser admin, UUID reservaId, String motivo);

    /**
     * Permite que o proprio morador cancele uma reserva sua, desde que ainda
     * nao tenha atingido o horario de inicio.
     */
    Reserva cancelarComoMorador(AuthenticatedUser morador, UUID reservaId);

    /**
     * Permite que o administrador cancele qualquer reserva do condominio,
     * desde que ainda nao tenha atingido o horario de inicio.
     */
    Reserva cancelarComoAdmin(AuthenticatedUser admin, UUID reservaId);
}