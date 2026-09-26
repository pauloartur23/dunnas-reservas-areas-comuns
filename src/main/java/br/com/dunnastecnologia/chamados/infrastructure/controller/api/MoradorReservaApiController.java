package br.com.dunnastecnologia.chamados.infrastructure.controller.api;

import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.WebControllerSupport;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.form.SolicitarReservaForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/morador/reservas")
@PreAuthorize("hasRole('MORADOR')")
public class MoradorReservaApiController {

    private final ReservaUseCases reservaUseCases;
    private final WebControllerSupport support;

    public MoradorReservaApiController(ReservaUseCases reservaUseCases, WebControllerSupport support) {
        this.reservaUseCases = reservaUseCases;
        this.support = support;
    }

    @PostMapping
    @Operation(summary = "Solicita a reserva de uma area comum", tags = "21 - Morador Web - Reservas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Reserva solicitada com sucesso e redirecionamento para o detalhe do registro."),
            @ApiResponse(responseCode = "400", description = "Dados informados sao invalidos ou violam regra de negocio."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado."),
            @ApiResponse(responseCode = "404", description = "Area comum nao encontrada.")
    })
    public String solicitarReserva(
            Authentication authentication,
            @ModelAttribute SolicitarReservaForm solicitarReservaForm,
            RedirectAttributes redirectAttributes
    ) {
        var currentUser = support.authenticatedUser(authentication);
        var reserva = reservaUseCases.solicitarReserva(
                currentUser,
                solicitarReservaForm.getAreaComumId(),
                solicitarReservaForm.getData(),
                solicitarReservaForm.getHoraInicio(),
                solicitarReservaForm.getHoraFim()
        );

        redirectAttributes.addFlashAttribute("successMessage", "Reserva solicitada com sucesso.");
        return "redirect:/morador/reservas/" + reserva.getId();
    }

    @PatchMapping("/{reservaId}/cancelar")
    @Operation(summary = "Cancela uma reserva do proprio morador", tags = "21 - Morador Web - Reservas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Reserva cancelada com sucesso e redirecionamento para o detalhe do registro."),
            @ApiResponse(responseCode = "400", description = "A reserva nao pode mais ser cancelada."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado."),
            @ApiResponse(responseCode = "404", description = "Reserva nao encontrada para o morador.")
    })
    public String cancelarReserva(
            Authentication authentication,
            @PathVariable UUID reservaId,
            RedirectAttributes redirectAttributes
    ) {
        reservaUseCases.cancelarComoMorador(support.authenticatedUser(authentication), reservaId);
        redirectAttributes.addFlashAttribute("successMessage", "Reserva cancelada com sucesso.");
        return "redirect:/morador/reservas/" + reservaId;
    }
}