package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.application.UserCase.AnexoChamadoUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.AnexoComentarioUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.ComentarioUseCase;
import br.com.dunnastecnologia.chamados.application.UserCase.MoradorUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.StatusChamadoUseCase;
import br.com.dunnastecnologia.chamados.application.UserCase.TipoChamadoUseCase;
import br.com.dunnastecnologia.chamados.application.pagination.PageResult;
import br.com.dunnastecnologia.chamados.domain.model.Chamado;
import br.com.dunnastecnologia.chamados.domain.model.Comentario;
import br.com.dunnastecnologia.chamados.domain.model.StatusChamado;
import br.com.dunnastecnologia.chamados.domain.model.TipoChamado;
import br.com.dunnastecnologia.chamados.domain.model.Unidade;
import br.com.dunnastecnologia.chamados.infrastructure.controller.api.MoradorChamadoApiController;
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

import java.util.Arrays;
import java.time.LocalDate;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({MoradorWebController.class, MoradorChamadoApiController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(WebControllerSupport.class)
class MoradorWebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MoradorUseCases moradorUseCases;

    @MockitoBean
    private TipoChamadoUseCase tipoChamadoUseCase;

    @MockitoBean
    private StatusChamadoUseCase statusChamadoUseCase;

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
    void listarChamadosDevePropagarFiltrosEPaginacaoDoMorador() throws Exception {
        UUID statusId = UUID.fromString("00000000-0000-0000-0000-000000000060");
        UUID unidadeId = UUID.fromString("00000000-0000-0000-0000-000000000061");
        UUID tipoChamadoId = UUID.fromString("00000000-0000-0000-0000-000000000062");
        LocalDate dataAbertura = LocalDate.of(2026, 4, 12);
        PageRequest pageRequest = PageRequest.of(3, 7);
        var morador = new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                "morador@condominio.local",
                "ROLE_MORADOR"
        );
        StatusChamado status = new StatusChamado();
        status.setId(statusId);
        status.setNome("Solicitado");
        Unidade unidade = new Unidade();
        unidade.setId(unidadeId);
        unidade.setIdentificacao("A-101");
        TipoChamado tipoChamado = new TipoChamado();
        tipoChamado.setId(tipoChamadoId);
        tipoChamado.setTitulo("Vazamento");
        tipoChamado.setPrazoHoras(24);

        when(moradorUseCases.listarMeusChamados(morador, statusId, unidadeId, tipoChamadoId, dataAbertura, pageRequest))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 3, 7));
        when(statusChamadoUseCase.listarStatus(PageRequest.of(0, 100)))
                .thenReturn(new PageResult<>(List.of(status), 1, 1, 0, 100));
        when(moradorUseCases.listarMinhasUnidades(morador, PageRequest.of(0, 100)))
                .thenReturn(new PageResult<>(List.of(unidade), 1, 1, 0, 100));
        when(tipoChamadoUseCase.listarTiposChamado(PageRequest.of(0, 100)))
                .thenReturn(new PageResult<>(List.of(tipoChamado), 1, 1, 0, 100));

        mockMvc.perform(
                        get("/morador/chamados")
                                .with(authentication(WebTestAuthenticationFactory.morador()))
                                .param("statusId", statusId.toString())
                                .param("unidadeId", unidadeId.toString())
                                .param("tipoChamadoId", tipoChamadoId.toString())
                                .param("dataAbertura", "2026-04-12")
                                .param("page", "3")
                                .param("size", "7")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("morador/chamados/lista"))
                .andExpect(model().attributeExists(
                        "chamados",
                        "chamadosPage",
                        "statusDisponiveis",
                        "unidadesDisponiveis",
                        "tiposChamadoDisponiveis"
                ));

        verify(moradorUseCases).listarMeusChamados(morador, statusId, unidadeId, tipoChamadoId, dataAbertura, pageRequest);
    }

    @Test
    void abrirChamadoDeveRedirecionarParaDetalheDoRegistroCriado() throws Exception {
        UUID unidadeId = UUID.fromString("00000000-0000-0000-0000-000000000040");
        UUID tipoChamadoId = UUID.fromString("00000000-0000-0000-0000-000000000041");
        UUID chamadoId = UUID.fromString("00000000-0000-0000-0000-000000000042");

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);

        when(moradorUseCases.abrirChamado(
                new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        "morador@condominio.local",
                        "ROLE_MORADOR"
                ),
                unidadeId,
                tipoChamadoId,
                "Vazamento na cozinha"
        )).thenReturn(chamado);

        mockMvc.perform(
                        multipart("/morador/chamados")
                                .with(authentication(WebTestAuthenticationFactory.morador()))
                                .param("unidadeId", unidadeId.toString())
                                .param("tipoChamadoId", tipoChamadoId.toString())
                                .param("descricao", "Vazamento na cozinha")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/morador/chamados/" + chamadoId));

        verify(moradorUseCases).abrirChamado(
                new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        "morador@condominio.local",
                        "ROLE_MORADOR"
                ),
                unidadeId,
                tipoChamadoId,
                "Vazamento na cozinha"
        );
    }

    @Test
    void abrirChamadoDevePermitirAnexoInicial() throws Exception {
        UUID unidadeId = UUID.fromString("00000000-0000-0000-0000-000000000044");
        UUID tipoChamadoId = UUID.fromString("00000000-0000-0000-0000-000000000045");
        UUID chamadoId = UUID.fromString("00000000-0000-0000-0000-000000000046");
        byte[] conteudo = "evidencia".getBytes();

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "foto.png",
                "image/png",
                conteudo
        );

        when(moradorUseCases.abrirChamado(
                new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        "morador@condominio.local",
                        "ROLE_MORADOR"
                ),
                unidadeId,
                tipoChamadoId,
                "Porta quebrada"
        )).thenReturn(chamado);

        mockMvc.perform(
                        multipart("/morador/chamados")
                                .file(arquivo)
                                .with(authentication(WebTestAuthenticationFactory.morador()))
                                .param("unidadeId", unidadeId.toString())
                                .param("tipoChamadoId", tipoChamadoId.toString())
                                .param("descricao", "Porta quebrada")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/morador/chamados/" + chamadoId));

        verify(anexoChamadoUseCases).adicionarAnexo(
                eq(new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        "morador@condominio.local",
                        "ROLE_MORADOR"
                )),
                eq(chamadoId),
                eq("foto.png"),
                eq("image/png"),
                eq((long) conteudo.length),
                argThat(bytes -> Arrays.equals(bytes, conteudo))
        );
    }

    @Test
    void adicionarAnexoDeveEncaminharMultipartParaCamadaDeAplicacao() throws Exception {
        UUID chamadoId = UUID.fromString("00000000-0000-0000-0000-000000000043");
        byte[] conteudo = "conteudo".getBytes();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "evidencia.txt",
                "text/plain",
                conteudo
        );

        mockMvc.perform(
                        multipart("/morador/chamados/{chamadoId}/anexos", chamadoId)
                                .file(arquivo)
                                .with(authentication(WebTestAuthenticationFactory.morador()))
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/morador/chamados/" + chamadoId));

        verify(anexoChamadoUseCases).adicionarAnexo(
                eq(new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        "morador@condominio.local",
                        "ROLE_MORADOR"
                )),
                eq(chamadoId),
                eq("evidencia.txt"),
                eq("text/plain"),
                eq((long) conteudo.length),
                argThat(bytes -> Arrays.equals(bytes, conteudo))
        );
    }

    @Test
    void comentarChamadoDevePermitirAnexoNoComentarioSomenteNoFluxoDoMorador() throws Exception {
        UUID chamadoId = UUID.fromString("00000000-0000-0000-0000-000000000047");
        UUID comentarioId = UUID.fromString("00000000-0000-0000-0000-000000000048");
        byte[] conteudo = "comentario-com-anexo".getBytes();

        Comentario comentario = new Comentario();
        comentario.setId(comentarioId);

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "observacao.pdf",
                "application/pdf",
                conteudo
        );

        when(moradorUseCases.comentarChamado(
                new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        "morador@condominio.local",
                        "ROLE_MORADOR"
                ),
                chamadoId,
                "Segue comprovante"
        )).thenReturn(comentario);

        mockMvc.perform(
                        multipart("/morador/chamados/{chamadoId}/comentarios", chamadoId)
                                .file(arquivo)
                                .with(authentication(WebTestAuthenticationFactory.morador()))
                                .param("mensagem", "Segue comprovante")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/morador/chamados/" + chamadoId));

        verify(anexoComentarioUseCases).adicionarAnexoAoComentario(
                eq(new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        "morador@condominio.local",
                        "ROLE_MORADOR"
                )),
                eq(chamadoId),
                eq(comentarioId),
                eq("observacao.pdf"),
                eq("application/pdf"),
                eq((long) conteudo.length),
                argThat(bytes -> Arrays.equals(bytes, conteudo))
        );
    }

    @Test
    void reabrirChamadoDeveRedirecionarParaDetalhe() throws Exception {
        UUID chamadoId = UUID.fromString("00000000-0000-0000-0000-000000000049");

        mockMvc.perform(
                        patch("/morador/chamados/{chamadoId}/reabrir", chamadoId)
                                .with(authentication(WebTestAuthenticationFactory.morador()))
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/morador/chamados/" + chamadoId));

        verify(moradorUseCases).reabrirChamado(
                new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        "morador@condominio.local",
                        "ROLE_MORADOR"
                ),
                chamadoId
        );
    }
}