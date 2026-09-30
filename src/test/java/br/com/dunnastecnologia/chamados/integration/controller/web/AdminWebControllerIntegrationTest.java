package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.application.UserCase.AdminUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.AnexoChamadoUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.AnexoComentarioUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.ComentarioUseCase;
import br.com.dunnastecnologia.chamados.application.pagination.PageResult;
import br.com.dunnastecnologia.chamados.domain.model.Bloco;
import br.com.dunnastecnologia.chamados.domain.model.Comentario;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.StatusChamado;
import br.com.dunnastecnologia.chamados.domain.model.Unidade;
import br.com.dunnastecnologia.chamados.infrastructure.controller.api.AdminChamadoApiController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.api.BlocoApiController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.api.ColaboradorTipoChamadoApiController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.api.MoradorUnidadeApiController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.api.StatusChamadoApiController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.api.TipoChamadoApiController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.api.UsuarioApiController;
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
import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static br.com.dunnastecnologia.chamados.infrastructure.controller.web.WebTestAuthenticationFactory.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({
        AdminWebController.class,
        AdminChamadoApiController.class,
        BlocoApiController.class,
        ColaboradorTipoChamadoApiController.class,
        MoradorUnidadeApiController.class,
        StatusChamadoApiController.class,
        TipoChamadoApiController.class,
        UsuarioApiController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(WebControllerSupport.class)
class AdminWebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUseCases adminUseCases;

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
        var admin = WebTestAuthenticationFactory.administrador();
        var usuario = new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "admin@condominio.local",
                "ROLE_ADMINISTRADOR"
        );
        UUID statusAtrasadoId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        StatusChamado atrasado = new StatusChamado();
        atrasado.setId(statusAtrasadoId);
        atrasado.setNome("Atrasado");

        when(adminUseCases.listarBlocos(PageRequest.of(0, 1))).thenReturn(new PageResult<>(List.of(), 0, 0, 0, 1));
        when(adminUseCases.listarUsuarios(PageRequest.of(0, 1))).thenReturn(new PageResult<>(List.of(), 0, 0, 0, 1));
        when(adminUseCases.listarTiposChamado(PageRequest.of(0, 1))).thenReturn(new PageResult<>(List.of(), 0, 0, 0, 1));
        when(adminUseCases.listarStatus(PageRequest.of(0, 1))).thenReturn(new PageResult<>(List.of(), 0, 0, 0, 1));
        when(adminUseCases.listarStatus(PageRequest.of(0, 100))).thenReturn(new PageResult<>(List.of(atrasado), 1, 1, 0, 100));
        when(adminUseCases.buscarChamados(usuario, null, null, null, PageRequest.of(0, 5)))
                .thenReturn(new PageResult<>(List.of(), 7, 2, 0, 5));
        when(adminUseCases.buscarChamados(usuario, statusAtrasadoId, null, null, PageRequest.of(0, 1)))
                .thenReturn(new PageResult<>(List.of(), 3, 3, 0, 1));

        mockMvc.perform(get("/admin").with(authentication(admin)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("totalChamadosAtrasados", 3L));
    }

    @Test
    void vinculosMoradorDeveExibirListasDeCadastradosESemUnidade() throws Exception {
        when(adminUseCases.listarMoradoresPorPrefixoEmail("ana", PageRequest.of(0, 100)))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 0, 100));
        when(adminUseCases.listarMoradoresPorPrefixoEmail("mar", PageRequest.of(1, 5)))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 1, 5));
        when(adminUseCases.listarBlocos(PageRequest.of(0, 100)))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 0, 100));
        when(adminUseCases.listarMoradoresSemUnidadePorPrefixoEmail("sem", PageRequest.of(0, 10)))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 0, 10));

        mockMvc.perform(
                        get("/admin/vinculos-morador")
                                .with(authentication(WebTestAuthenticationFactory.administrador()))
                                .param("moradorEmail", "ana")
                                .param("cadastradosEmail", "mar")
                                .param("cadastradosPage", "1")
                                .param("cadastradosSize", "5")
                                .param("semUnidadeEmail", "sem")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("admin/vinculos-morador/lista"))
                .andExpect(model().attributeExists(
                        "moradoresDisponiveis",
                        "moradoresCadastrados",
                        "moradoresCadastradosPage",
                        "moradoresSemUnidade",
                        "moradoresSemUnidadePage"
                ));
    }

    @Test
    void listarBlocosDeveEncaminharPaginacaoParaCasoDeUso() throws Exception {
        PageRequest pageRequest = PageRequest.of(2, 15);
        when(adminUseCases.listarBlocos(pageRequest))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 2, 15));

        mockMvc.perform(
                        get("/admin/blocos")
                                .with(authentication(WebTestAuthenticationFactory.administrador()))
                                .param("page", "2")
                                .param("size", "15")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("admin/blocos/lista"))
                .andExpect(model().attributeExists("blocos", "blocosPage"));

        verify(adminUseCases).listarBlocos(pageRequest);
    }

    @Test
    void detalharBlocoDeveExibirMoradoresDasUnidadesNoModel() throws Exception {
        UUID blocoId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID unidadeId = UUID.fromString("00000000-0000-0000-0000-000000000012");

        Bloco bloco = new Bloco();
        bloco.setId(blocoId);
        bloco.setIdentificacao("Bloco A");
        bloco.setQuantidadeAndares(5);
        bloco.setApartamentosPorAndar(4);

        Unidade unidade = new Unidade();
        unidade.setId(unidadeId);
        unidade.setIdentificacao("A-101");
        unidade.setAndar(1);
        unidade.setBloco(bloco);

        Morador morador = new Morador();
        morador.setId(UUID.fromString("00000000-0000-0000-0000-000000000013"));
        morador.setNome("Ana Silva");
        morador.setEmail("ana@condominio.local");

        when(adminUseCases.buscarBlocoPorId(blocoId)).thenReturn(bloco);
        when(adminUseCases.listarUnidadesDoBloco(blocoId, PageRequest.of(0, 20)))
                .thenReturn(new PageResult<>(List.of(unidade), 1, 1, 0, 20));
        when(adminUseCases.listarMoradoresPorUnidadeIds(List.of(unidadeId)))
                .thenReturn(Map.of(unidadeId, List.of(morador)));

        mockMvc.perform(
                        get("/admin/blocos/{blocoId}", blocoId)
                                .with(authentication(WebTestAuthenticationFactory.administrador()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("admin/blocos/detalhe"))
                .andExpect(model().attributeExists("bloco", "unidades", "unidadesPage"))
                .andExpect(model().attribute("unidades", hasItem(hasEntry("moradores", List.of(Map.of(
                        "id", morador.getId(),
                        "nome", morador.getNome(),
                        "email", morador.getEmail(),
                        "role", morador.getRole(),
                        "tipo", "Morador"
                ))))));
    }

    @Test
    void listarChamadosDevePropagarFiltrosParaCamadaDeAplicacao() throws Exception {
        UUID statusId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        LocalDate dataAbertura = LocalDate.of(2026, 4, 10);
        PageRequest pageRequest = PageRequest.of(1, 5);
        when(adminUseCases.buscarChamados(
                new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "admin@condominio.local",
                        "ROLE_ADMINISTRADOR"
                ),
                statusId,
                "Ana",
                dataAbertura,
                pageRequest
        )).thenReturn(new PageResult<>(List.of(), 0, 0, 1, 5));
        when(adminUseCases.listarStatus(PageRequest.of(0, 100)))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 0, 100));

        mockMvc.perform(
                        get("/admin/chamados")
                                .with(authentication(WebTestAuthenticationFactory.administrador()))
                                .param("statusId", statusId.toString())
                                .param("moradorNome", "Ana")
                                .param("dataAbertura", "2026-04-10")
                                .param("page", "1")
                                .param("size", "5")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("admin/chamados/lista"))
                .andExpect(model().attributeExists("chamados", "chamadosPage", "statusDisponiveis"));

        verify(adminUseCases).buscarChamados(
                new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "admin@condominio.local",
                        "ROLE_ADMINISTRADOR"
                ),
                statusId,
                "Ana",
                dataAbertura,
                pageRequest
        );
    }

    @Test
    void listarStatusChamadoDeveMarcarStatusReservadoComoNaoEditavel() throws Exception {
        UUID statusId = UUID.fromString("00000000-0000-0000-0000-000000000014");

        StatusChamado statusReservado = new StatusChamado();
        statusReservado.setId(statusId);
        statusReservado.setNome("Finalizado");
        statusReservado.setInicialPadrao(Boolean.FALSE);

        when(adminUseCases.listarStatus(PageRequest.of(0, 10)))
                .thenReturn(new PageResult<>(List.of(statusReservado), 1, 1, 0, 10));
        when(adminUseCases.buscarStatusPorId(statusId)).thenReturn(statusReservado);

        mockMvc.perform(
                        get("/admin/status-chamado")
                                .with(authentication(WebTestAuthenticationFactory.administrador()))
                                .param("statusId", statusId.toString())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("admin/status-chamado/lista"))
                .andExpect(model().attribute("statusEdicaoBloqueada", true))
                .andExpect(model().attribute("statusChamado", hasItem(hasEntry("editavel", false))));
    }

    @Test
    void removerUsuarioDeveRedirecionarParaListaAposSoftDelete() throws Exception {
        UUID usuarioId = UUID.fromString("00000000-0000-0000-0000-000000000020");

        mockMvc.perform(
                        delete("/admin/usuarios/{usuarioId}", usuarioId)
                                .with(authentication(WebTestAuthenticationFactory.administrador()))
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"));

        verify(adminUseCases).removerUsuario(
                new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "admin@condominio.local",
                        "ROLE_ADMINISTRADOR"
                ),
                usuarioId
        );
    }

    @Test
    void comentarChamadoDoAdminDevePermitirAnexoNoComentario() throws Exception {
        UUID chamadoId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        UUID comentarioId = UUID.fromString("00000000-0000-0000-0000-000000000022");
        byte[] conteudo = "comentario-admin".getBytes();

        Comentario comentario = new Comentario();
        comentario.setId(comentarioId);

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "analise.pdf",
                "application/pdf",
                conteudo
        );

        when(adminUseCases.comentarChamado(
                new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "admin@condominio.local",
                        "ROLE_ADMINISTRADOR"
                ),
                chamadoId,
                "Segue analise"
        )).thenReturn(comentario);

        mockMvc.perform(
                        multipart("/admin/chamados/{chamadoId}/comentarios", chamadoId)
                                .file(arquivo)
                                .with(authentication(WebTestAuthenticationFactory.administrador()))
                                .param("mensagem", "Segue analise")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/chamados/" + chamadoId));

        verify(anexoComentarioUseCases).adicionarAnexoAoComentario(
                eq(new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "admin@condominio.local",
                        "ROLE_ADMINISTRADOR"
                )),
                eq(chamadoId),
                eq(comentarioId),
                eq("analise.pdf"),
                eq("application/pdf"),
                eq((long) conteudo.length),
                argThat(bytes -> Arrays.equals(bytes, conteudo))
        );
    }
}