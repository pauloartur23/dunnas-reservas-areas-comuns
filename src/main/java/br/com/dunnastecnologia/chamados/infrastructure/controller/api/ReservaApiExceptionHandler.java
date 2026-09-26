package br.com.dunnastecnologia.chamados.infrastructure.controller.api;

import br.com.dunnastecnologia.chamados.infrastructure.controller.web.WebControllerSupport;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.exception.ResourceNotFoundException;
import br.com.dunnastecnologia.chamados.infrastructure.exception.UnauthorizedOperationException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Tratador de excecoes dedicado aos controllers de Reserva do pacote "api".
 * O WebExceptionHandler existente cobre apenas o pacote "controller.web" e nao
 * captura excecoes lancadas pelos controllers "api" (incluindo os de chamados
 * ja existentes, fora do escopo desta funcionalidade). Em vez de ampliar o
 * escopo do handler existente - o que alteraria o comportamento observavel
 * dos chamados - eu criei este handler isolado, restrito por assignableTypes
 * exatamente aos controllers novos de Reserva, sem tocar em nada pre-existente.
 */
@ControllerAdvice(assignableTypes = {
        MoradorReservaApiController.class,
        AdminReservaApiController.class
})
@Hidden
public class ReservaApiExceptionHandler {

    private final WebControllerSupport support;

    public ReservaApiExceptionHandler(WebControllerSupport support) {
        this.support = support;
    }

    @ExceptionHandler({
            BusinessRuleException.class,
            ResourceNotFoundException.class,
            UnauthorizedOperationException.class,
            IllegalArgumentException.class
    })
    public String handleKnownExceptions(
            RuntimeException exception,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }

        if (support.isAuthenticated(authentication)) {
            return "redirect:" + support.homePathForRole(support.authenticatedUser(authentication).role());
        }

        return "redirect:/login";
    }
}