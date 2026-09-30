package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.application.UserCase.AnexoChamadoUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.AnexoComentarioUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.ColaboradorUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.ComentarioUseCase;
import br.com.dunnastecnologia.chamados.application.pagination.PageResult;
import br.com.dunnastecnologia.chamados.domain.model.Chamado;
import br.com.dunnastecnologia.chamados.domain.model.Comentario;
import br.com.dunnastecnologia.chamados.domain.model.StatusChamado;
import br.com.dunnastecnologia.chamados.infrastructure.controller.api.ColaboradorChamadoApiController;
import br.com.dunnastecnologia.chamados.infrastructure.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static br.com.dunnastecnologia.chamados.infrastructure.controller.web.WebTestAuthenticationFactory.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({ColaboradorWebController.class, ColaboradorChamadoApiController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(WebControllerSupport.class)
class ColaboradorWebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ColaboradorUseCases colaboradorUseCases;

    @MockitoBean
    private AnexoChamadoUseCases anexoChamadoUseCases;

    @MockitoBean
    private AnexoComentarioUseCases anexoComentarioUseCases;

    @MockitoBean
    private ComentarioUseCase comentarioUseCase;

    /**
     * Dublê do JwtService, necessário apenas para o Spring conseguir montar o contexto
     * de teste: o JwtAuthenticationFilter (um Filter, então é escaneado pelo @WebMvcTest)
     * depende dele no construtor. Como @AutoConfigureMockMvc(addFilters = false) desliga
     * a execução do filtro nas requisições de teste, esse mock nunca é chamado de verdade.
     */
    @MockitoBean
    private JwtService jwtService;

    /**
     * Garante que a autenticacao simulada de um teste (colocada diretamente no
     * SecurityContextHolder por WebTestAuthenticationFactory.authentication()) nao
     * "vaze" para o proximo teste, ja que aqui nao existe filtro pra limpar isso
     * automaticamente entre as requisicoes.
     */
    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboardDeveExibirContadorDeChamadosAtrasados() throws Exception {
        var colaboradorAuth = WebTestAuthenticationFactory.colaborador();
        var colaborador = new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "colaborador@condominio.local",
                "ROLE_COLABORADOR"
        );
        UUID statusAtrasadoId = UUID.fromString("00000000-0000-0000-0000-000000000098");

        StatusChamado atrasado = new StatusChamado();
        atrasado.setId(statusAtrasadoId);
        atrasado.setNome("Atrasado");

        when(colaboradorUseCases.listarStatusDisponiveis(colaborador, PageRequest.of(0, 100)))
                .thenReturn(new PageResult<>(List.of(atrasado), 1, 1, 0, 100));
        when(colaboradorUseCases.buscarChamados(colaborador, null, null, null, null, PageRequest.of(0, 5)))
                .thenReturn(new PageResult<>(List.of(), 6, 2, 0, 5));
        when(colaboradorUseCases.buscarChamados(colaborador, statusAtrasadoId, null, null, null, PageRequest.of(0, 1)))
                .thenReturn(new PageResult<>(List.of(), 2, 2, 0, 1));

        mockMvc.perform(get("/colaborador").with(authentication(colaboradorAuth)))
                .andExpect(status().isOk())
                .andExpect(view().name("colaborador/dashboard"))
                .andExpect(model().attribute("totalChamadosAtrasados", 2L));
    }

    @Test
    void listarChamadosDevePropagarFiltrosEPaginacaoDoColaborador() throws Exception {
        UUID statusId = UUID.fromString("00000000-0000-0000-0000-000000000030");
        UUID tipoChamadoId = UUID.fromString("00000000-0000-0000-0000-000000000031");
        LocalDate dataAbertura = LocalDate.of(2026, 4, 9);
        PageRequest pageRequest = PageRequest.of(1, 12);
        var colaborador = new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "colaborador@condominio.local",
                "ROLE_COLABORADOR"
        );

        when(colaboradorUseCases.buscarChamados(colaborador, statusId, tipoChamadoId, "A-101", dataAbertura, pageRequest))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 1, 12));
        when(colaboradorUseCases.listarStatusDisponiveis(colaborador, PageRequest.of(0, 100)))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 0, 100));
        when(colaboradorUseCases.listarTiposChamadoDisponiveis(colaborador, PageRequest.of(0, 100)))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 0, 100));

        mockMvc.perform(
                        get("/colaborador/chamados")
                                .with(authentication(WebTestAuthenticationFactory.colaborador()))
                                .param("statusId", statusId.toString())
                                .param("tipoChamadoId", tipoChamadoId.toString())
                                .param("unidade", "A-101")
                                .param("dataAbertura", "2026-04-09")
                                .param("page", "1")
                                .param("size", "12")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("colaborador/chamados/lista"))
                .andExpect(model().attributeExists("chamados", "chamadosPage", "statusDisponiveis", "tiposChamadoDisponiveis"));

        verify(colaboradorUseCases).buscarChamados(colaborador, statusId, tipoChamadoId, "A-101", dataAbertura, pageRequest);
    }

    @Test
    void comentarChamadoDoColaboradorDevePermitirAnexoNoComentario() throws Exception {
        UUID chamadoId = UUID.fromString("00000000-0000-0000-0000-000000000032");
        UUID comentarioId = UUID.fromString("00000000-0000-0000-0000-000000000033");
        byte[] conteudo = "comentario-colaborador".getBytes();
        var colaborador = new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "colaborador@condominio.local",
                "ROLE_COLABORADOR"
        );

        Comentario comentario = new Comentario();
        comentario.setId(comentarioId);

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "retorno.txt",
                "text/plain",
                conteudo
        );

        when(colaboradorUseCases.comentarChamado(colaborador, chamadoId, "Atualizacao do atendimento"))
                .thenReturn(comentario);

        mockMvc.perform(
                        multipart("/colaborador/chamados/{chamadoId}/comentarios", chamadoId)
                                .file(arquivo)
                                .with(authentication(WebTestAuthenticationFactory.colaborador()))
                                .param("mensagem", "Atualizacao do atendimento")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/colaborador/chamados/" + chamadoId));

        verify(anexoComentarioUseCases).adicionarAnexoAoComentario(
                eq(colaborador),
                eq(chamadoId),
                eq(comentarioId),
                eq("retorno.txt"),
                eq("text/plain"),
                eq((long) conteudo.length),
                argThat(bytes -> Arrays.equals(bytes, conteudo))
        );
    }

    @Test
    void atualizarStatusDeveRedirecionarParaListaQuandoChamadoForFinalizado() throws Exception {
        UUID chamadoId = UUID.fromString("00000000-0000-0000-0000-000000000034");
        UUID statusId = UUID.fromString("00000000-0000-0000-0000-000000000035");
        var colaborador = new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "colaborador@condominio.local",
                "ROLE_COLABORADOR"
        );

        Chamado chamadoFinalizado = new Chamado();
        chamadoFinalizado.setId(chamadoId);
        chamadoFinalizado.setDataFinalizacao(java.time.LocalDateTime.now());

        when(colaboradorUseCases.atualizarStatusChamado(colaborador, chamadoId, statusId))
                .thenReturn(chamadoFinalizado);

        mockMvc.perform(
                        patch("/colaborador/chamados/{chamadoId}/status", chamadoId)
                                .with(authentication(WebTestAuthenticationFactory.colaborador()))
                                .param("statusId", statusId.toString())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/colaborador/chamados"));
    }
}