package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.domain.model.Administrador;
import br.com.dunnastecnologia.chamados.domain.model.Colaborador;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.Usuario;
import br.com.dunnastecnologia.chamados.infrastructure.security.adapter.UserDetailsImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

final class WebTestAuthenticationFactory {

    private WebTestAuthenticationFactory() {
    }

    static UsernamePasswordAuthenticationToken administrador() {
        return authenticationFor(usuarioAdministrador());
    }

    static UsernamePasswordAuthenticationToken colaborador() {
        return authenticationFor(usuarioColaborador());
    }

    static UsernamePasswordAuthenticationToken morador() {
        return authenticationFor(usuarioMorador());
    }

    /**
     * Substitui o SecurityMockMvcRequestPostProcessors.authentication() padrao do
     * Spring Security Test. Aquele padrao guarda a autenticacao na sessao e depende
     * de um filtro do Spring Security rodar durante a requisicao pra "resgatar" essa
     * autenticacao e disponibiliza-la pro controller. Como estes testes usam
     * @AutoConfigureMockMvc(addFilters = false) (desliga todos os filtros, nao so o
     * JwtAuthenticationFilter), esse resgate nunca acontecia e o controller sempre via
     * a requisicao como nao autenticada.
     *
     * A correcao tem duas partes, porque o Spring le o usuario autenticado de dois
     * jeitos diferentes dependendo do controller:
     *
     * 1) SecurityContextHolder.setContext(...) - usado quando algum codigo pergunta
     * "quem esta autenticado agora" diretamente ao Spring Security.
     *
     * 2) request.setUserPrincipal(...) - usado pelo proprio Spring MVC quando um
     * metodo de controller declara um parametro do tipo Authentication (ou Principal).
     * Normalmente quem preenche isso e um filtro do Spring Security, que tambem esta
     * desligado aqui.
     *
     * Fazendo as duas coisas, a autenticacao simulada funciona independente de qual
     * caminho o controller usa pra descobrir quem esta logado, e sem precisar de
     * nenhum filtro ligado.
     */
    static RequestPostProcessor authentication(Authentication authentication) {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            request.setUserPrincipal(authentication);
            return request;
        };
    }

    private static UsernamePasswordAuthenticationToken authenticationFor(Usuario usuario) {
        UserDetailsImpl principal = new UserDetailsImpl(usuario);
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                usuario.getSenha(),
                principal.getAuthorities()
        );
    }

    private static Administrador usuarioAdministrador() {
        Administrador administrador = new Administrador();
        administrador.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        administrador.setNome("Administrador");
        administrador.setEmail("admin@condominio.local");
        administrador.setSenha("senha");
        return administrador;
    }

    private static Colaborador usuarioColaborador() {
        Colaborador colaborador = new Colaborador();
        colaborador.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        colaborador.setNome("Colaborador");
        colaborador.setEmail("colaborador@condominio.local");
        colaborador.setSenha("senha");
        return colaborador;
    }

    private static Morador usuarioMorador() {
        Morador morador = new Morador();
        morador.setId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        morador.setNome("Morador");
        morador.setEmail("morador@condominio.local");
        morador.setSenha("senha");
        return morador;
    }
}