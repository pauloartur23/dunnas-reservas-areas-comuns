package br.com.dunnastecnologia.chamados.infrastructure.controller.api;

import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.WebControllerSupport;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.form.CadastrarAreaComumForm;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.form.NegarReservaForm;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminReservaApiController {

    private final ReservaUseCases reservaUseCases;
    private final WebControllerSupport support;

    public AdminReservaApiController(ReservaUseCases reservaUseCases, WebControllerSupport support) {
        this.reservaUseCases = reservaUseCases;
        this.support = support;
    }

    @PostMapping("/admin/areas-comuns")
    @Operation(summary = "Cadastra uma nova area comum", tags = "23 - Admin Web - Reservas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Area comum cadastrada com sucesso e redirecionamento para a listagem."),
            @ApiResponse(responseCode = "400", description = "Dados informados sao invalidos."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado.")
    })
    public String cadastrarAreaComum(
            Authentication authentication,
            @ModelAttribute CadastrarAreaComumForm cadastrarAreaComumForm,
            RedirectAttributes redirectAttributes
    ) {
        reservaUseCases.cadastrarAreaComum(
                support.authenticatedUser(authentication),
                cadastrarAreaComumForm.getNome(),
                cadastrarAreaComumForm.getDescricao()
        );
        redirectAttributes.addFlashAttribute("successMessage", "Area comum cadastrada com sucesso.");
        return "redirect:/admin/areas-comuns";
    }

    @PatchMapping("/admin/areas-comuns/{areaComumId}/disponibilidade")
    @Operation(summary = "Ativa ou desativa uma area comum", tags = "23 - Admin Web - Reservas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Disponibilidade atualizada com sucesso e redirecionamento para a listagem."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado."),
            @ApiResponse(responseCode = "404", description = "Area comum nao encontrada.")
    })
    public String alterarDisponibilidadeAreaComum(
            Authentication authentication,
            @PathVariable UUID areaComumId,
            @RequestParam boolean ativa,
            RedirectAttributes redirectAttributes
    ) {
        reservaUseCases.alterarDisponibilidadeAreaComum(
                support.authenticatedUser(authentication), areaComumId, ativa
        );
        redirectAttributes.addFlashAttribute("successMessage", "Area comum atualizada com sucesso.");
        return "redirect:/admin/areas-comuns";
    }

    @PatchMapping("/admin/reservas/{reservaId}/aprovar")
    @Operation(summary = "Aprova uma solicitacao de reserva", tags = "23 - Admin Web - Reservas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Reserva aprovada com sucesso e redirecionamento para a listagem."),
            @ApiResponse(responseCode = "400", description = "Existe reserva aprovada conflitante ou a reserva nao esta mais solicitada."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado."),
            @ApiResponse(responseCode = "404", description = "Reserva nao encontrada.")
    })
    public String aprovarReserva(
            Authentication authentication,
            @PathVariable UUID reservaId,
            RedirectAttributes redirectAttributes
    ) {
        reservaUseCases.aprovarReserva(support.authenticatedUser(authentication), reservaId);
        redirectAttributes.addFlashAttribute("successMessage", "Reserva aprovada com sucesso.");
        return "redirect:/admin/reservas";
    }

    @PatchMapping("/admin/reservas/{reservaId}/negar")
    @Operation(summary = "Nega uma solicitacao de reserva, exigindo motivo", tags = "23 - Admin Web - Reservas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Reserva negada com sucesso e redirecionamento para a listagem."),
            @ApiResponse(responseCode = "400", description = "Motivo nao informado ou reserva nao esta mais solicitada."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado."),
            @ApiResponse(responseCode = "404", description = "Reserva nao encontrada.")
    })
    public String negarReserva(
            Authentication authentication,
            @PathVariable UUID reservaId,
            @ModelAttribute NegarReservaForm negarReservaForm,
            RedirectAttributes redirectAttributes
    ) {
        reservaUseCases.negarReserva(
                support.authenticatedUser(authentication), reservaId, negarReservaForm.getMotivo()
        );
        redirectAttributes.addFlashAttribute("successMessage", "Reserva negada.");
        return "redirect:/admin/reservas";
    }

    @PatchMapping("/admin/reservas/{reservaId}/cancelar")
    @Operation(summary = "Cancela qualquer reserva do condominio", tags = "23 - Admin Web - Reservas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Reserva cancelada com sucesso e redirecionamento para a listagem."),
            @ApiResponse(responseCode = "400", description = "A reserva nao pode mais ser cancelada."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado."),
            @ApiResponse(responseCode = "404", description = "Reserva nao encontrada.")
    })
    public String cancelarReserva(
            Authentication authentication,
            @PathVariable UUID reservaId,
            RedirectAttributes redirectAttributes
    ) {
        reservaUseCases.cancelarComoAdmin(support.authenticatedUser(authentication), reservaId);
        redirectAttributes.addFlashAttribute("successMessage", "Reserva cancelada com sucesso.");
        return "redirect:/admin/reservas";
    }
}