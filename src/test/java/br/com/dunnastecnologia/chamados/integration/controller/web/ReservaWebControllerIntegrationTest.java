package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.application.pagination.PageResult;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.infrastructure.controller.api.AdminReservaApiController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.api.MoradorReservaApiController;
import br.com.dunnastecnologia.chamados.infrastructure.exception.ResourceNotFoundException;
import br.com.dunnastecnologia.chamados.infrastructure.security.JwtService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static br.com.dunnastecnologia.chamados.infrastructure.controller.web.WebTestAuthenticationFactory.authentication;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Cobre o requisito de teste do desafio: "Exercitar autorizacao por MORADOR,
 * ADMINISTRADOR e COLABORADOR, incluindo acesso indevido e tentativa de
 * consultar dados de outro morador" - para os 4 controllers novos de Reserva.
 * O ReservaServiceTest ja cobre as regras de negocio em isolamento (com o
 * validador de autorizacao mockado); este teste prova que o @PreAuthorize de
 * cada controller esta de fato ligado ao papel certo, na camada HTTP real, e
 * que um morador nao enxerga nem altera dados de outro morador.
 *
 * @EnableMethodSecurity e repetido aqui de proposito: no projeto real ele
 * esta no SecurityConfig, mas o @WebMvcTest so carrega os controllers
 * listados, nunca o SecurityConfig - sem repeti-lo aqui, o @PreAuthorize dos
 * controllers fica inerte e qualquer perfil passa (confirmado na primeira
 * versao deste teste: os casos de bloqueio executavam o metodo de verdade e
 * quebravam com NullPointerException no mock sem stub, em vez de barrar).
 *
 * addFilters = false desliga a cadeia de filtros do Spring Security (mesmo
 * padrao usado em WebTestAuthenticationFactory para os testes de Chamado).
 * Sem o ExceptionTranslationFilter (que e' quem normalmente converte
 * AccessDeniedException em HTTP 403), a excecao de autorizacao chega ao
 * FrameworkServlet sem resolver e e' embrulhada em ServletException - e' por
 * isso que os testes de acesso indevido verificam a causa da ServletException,
 * nao um status HTTP. Em producao, com os filtros ligados, e essa mesma
 * AccessDeniedException que vira 403.
 */
@WebMvcTest({
        MoradorReservaWebController.class,
        MoradorReservaApiController.class,
        AdminReservaWebController.class,
        AdminReservaApiController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(WebControllerSupport.class)
@EnableMethodSecurity
class ReservaWebControllerIntegrationTest {

    private static final UUID AREA_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID RESERVA_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    private static final AuthenticatedUser MORADOR = new AuthenticatedUser(
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            "morador@condominio.local",
            "ROLE_MORADOR"
    );

    private static final AuthenticatedUser ADMINISTRADOR = new AuthenticatedUser(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "admin@condominio.local",
            "ROLE_ADMINISTRADOR"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaUseCases reservaUseCases;

    /**
     * Mesmo motivo do ColaboradorWebControllerIntegrationTest: o
     * JwtAuthenticationFilter (escaneado por ser um Filter) exige um
     * JwtService no construtor para o contexto de teste montar; como os
     * filtros estao desligados, ele nunca e chamado de verdade.
     */
    @MockitoBean
    private JwtService jwtService;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Confirma que uma requisicao foi barrada pelo @PreAuthorize: a excecao
     * chega como ServletException (ver Javadoc da classe), cuja causa precisa
     * ser AccessDeniedException - e nao, por exemplo, um NullPointerException
     * de o metodo real ter executado sem estar protegido.
     */
    private void assertAcessoNegado(Executable action) {
        ServletException exception = assertThrows(ServletException.class, action);
        assertTrue(
                exception.getCause() instanceof AccessDeniedException,
                () -> "Esperava AccessDeniedException como causa da ServletException, mas veio: " + exception.getCause()
        );
    }

    // ---------- caminho feliz: cada perfil acessa o que pode acessar ----------

    @Test
    void moradorConsegueListarAreasComuns() throws Exception {
        when(reservaUseCases.listarAreasComuns()).thenReturn(List.of());

        mockMvc.perform(get("/morador/areas-comuns").with(authentication(WebTestAuthenticationFactory.morador())))
                .andExpect(status().isOk())
                .andExpect(view().name("morador/areas-comuns/lista"));
    }

    @Test
    void moradorConsegueSolicitarReserva() throws Exception {
        LocalDate data = LocalDate.of(2026, 12, 1);
        LocalTime horaInicio = LocalTime.of(18, 0);
        LocalTime horaFim = LocalTime.of(20, 0);

        Reserva reservaCriada = new Reserva();
        reservaCriada.setId(RESERVA_ID);

        when(reservaUseCases.solicitarReserva(eq(MORADOR), eq(AREA_ID), eq(data), eq(horaInicio), eq(horaFim)))
                .thenReturn(reservaCriada);

        mockMvc.perform(post("/morador/reservas")
                        .with(authentication(WebTestAuthenticationFactory.morador()))
                        .param("areaComumId", AREA_ID.toString())
                        .param("data", "2026-12-01")
                        .param("horaInicio", "18:00")
                        .param("horaFim", "20:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/morador/reservas/" + RESERVA_ID));
    }

    @Test
    void administradorConsegueListarReservas() throws Exception {
        when(reservaUseCases.listarReservasParaAdmin(eq(ADMINISTRADOR), isNull(), isNull(), isNull(), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 0, 10));
        when(reservaUseCases.listarAreasComuns()).thenReturn(List.of());

        mockMvc.perform(get("/admin/reservas").with(authentication(WebTestAuthenticationFactory.administrador())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservas/lista"));
    }

    @Test
    void administradorConsegueAprovarReserva() throws Exception {
        when(reservaUseCases.aprovarReserva(ADMINISTRADOR, RESERVA_ID)).thenReturn(new Reserva());

        mockMvc.perform(patch("/admin/reservas/{reservaId}/aprovar", RESERVA_ID)
                        .with(authentication(WebTestAuthenticationFactory.administrador())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reservas"));
    }

    // ---------- acesso indevido entre perfis ("Controle de acesso e permissoes") ----------

    @Test
    void colaboradorNaoAcessaAreasComunsDoMorador() {
        assertAcessoNegado(() -> mockMvc.perform(
                get("/morador/areas-comuns").with(authentication(WebTestAuthenticationFactory.colaborador()))
        ));
        verifyNoInteractions(reservaUseCases);
    }

    @Test
    void colaboradorNaoSolicitaReserva() {
        assertAcessoNegado(() -> mockMvc.perform(
                post("/morador/reservas").with(authentication(WebTestAuthenticationFactory.colaborador()))
        ));
        verifyNoInteractions(reservaUseCases);
    }

    @Test
    void colaboradorNaoAcessaReservasDoAdmin() {
        assertAcessoNegado(() -> mockMvc.perform(
                get("/admin/reservas").with(authentication(WebTestAuthenticationFactory.colaborador()))
        ));
        verifyNoInteractions(reservaUseCases);
    }

    @Test
    void colaboradorNaoAprovaReserva() {
        assertAcessoNegado(() -> mockMvc.perform(
                patch("/admin/reservas/{reservaId}/aprovar", RESERVA_ID)
                        .with(authentication(WebTestAuthenticationFactory.colaborador()))
        ));
        verifyNoInteractions(reservaUseCases);
    }

    @Test
    void administradorNaoAcessaReservasComoMorador() {
        assertAcessoNegado(() -> mockMvc.perform(
                get("/morador/reservas").with(authentication(WebTestAuthenticationFactory.administrador()))
        ));
        verifyNoInteractions(reservaUseCases);
    }

    @Test
    void moradorNaoCadastraAreaComum() {
        assertAcessoNegado(() -> mockMvc.perform(
                post("/admin/areas-comuns").with(authentication(WebTestAuthenticationFactory.morador()))
        ));
        verifyNoInteractions(reservaUseCases);
    }

    @Test
    void moradorNaoAprovaReservaPeloEndpointDoAdmin() {
        assertAcessoNegado(() -> mockMvc.perform(
                patch("/admin/reservas/{reservaId}/aprovar", RESERVA_ID)
                        .with(authentication(WebTestAuthenticationFactory.morador()))
        ));
        verifyNoInteractions(reservaUseCases);
    }

    @Test
    void moradorNaoNegaReservaPeloEndpointDoAdmin() {
        assertAcessoNegado(() -> mockMvc.perform(
                patch("/admin/reservas/{reservaId}/negar", RESERVA_ID)
                        .with(authentication(WebTestAuthenticationFactory.morador()))
        ));
        verifyNoInteractions(reservaUseCases);
    }

    // ---------- isolamento de dados entre moradores (RNF-03 / CA-01-13) ----------

    @Test
    void moradorNaoVisualizaReservaDeOutroMorador() throws Exception {
        when(reservaUseCases.buscarMinhaReserva(MORADOR, RESERVA_ID))
                .thenThrow(new ResourceNotFoundException("Reserva nao encontrada para o morador"));

        mockMvc.perform(get("/morador/reservas/{reservaId}", RESERVA_ID)
                        .with(authentication(WebTestAuthenticationFactory.morador())))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void moradorNaoCancelaReservaDeOutroMorador() throws Exception {
        when(reservaUseCases.cancelarComoMorador(MORADOR, RESERVA_ID))
                .thenThrow(new ResourceNotFoundException("Reserva nao encontrada para o morador"));

        mockMvc.perform(patch("/morador/reservas/{reservaId}/cancelar", RESERVA_ID)
                        .with(authentication(WebTestAuthenticationFactory.morador())))
                .andExpect(status().is3xxRedirection());
    }
}