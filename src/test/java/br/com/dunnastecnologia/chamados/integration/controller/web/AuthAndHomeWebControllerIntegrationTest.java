package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.infrastructure.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static br.com.dunnastecnologia.chamados.infrastructure.controller.web.WebTestAuthenticationFactory.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({AuthWebController.class, HomeWebController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(WebControllerSupport.class)
class AuthAndHomeWebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
     * "vaze" para o proximo teste. Nesta classe isso e ainda mais importante, porque
     * ha um teste que espera NENHUMA autenticacao (usuario anonimo) - sem esta limpeza,
     * ele poderia herdar por engano a autenticacao deixada por outro teste da classe.
     */
    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void homeDeveRedirecionarParaLoginQuandoNaoHaSessaoAutenticada() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void homeDeveRedirecionarAdministradorParaPainelCorreto() throws Exception {
        mockMvc.perform(get("/").with(authentication(WebTestAuthenticationFactory.administrador())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void loginDeveRenderizarTelaPublica() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }
}