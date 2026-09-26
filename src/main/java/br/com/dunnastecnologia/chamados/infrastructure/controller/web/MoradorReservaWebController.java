package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import br.com.dunnastecnologia.chamados.infrastructure.controller.web.form.SolicitarReservaForm;

@Controller
@RequestMapping("/morador")
@PreAuthorize("hasRole('MORADOR')")
public class MoradorReservaWebController {

    private final ReservaUseCases reservaUseCases;
    private final WebControllerSupport support;

    public MoradorReservaWebController(ReservaUseCases reservaUseCases, WebControllerSupport support) {
        this.reservaUseCases = reservaUseCases;
        this.support = support;
    }

    @ModelAttribute("solicitarReservaForm")
    public SolicitarReservaForm solicitarReservaForm() {
        return new SolicitarReservaForm();
    }

    @Operation(summary = "Exibe as areas comuns e a consulta de disponibilidade", tags = "20 - Morador Web - Reservas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagina de areas comuns renderizada com sucesso."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado."),
            @ApiResponse(responseCode = "404", description = "Area comum informada nao encontrada.")
    })
    @GetMapping({"/areas-comuns", "/areas-comuns/"})
    @Transactional(readOnly = true)
    public String listarAreasComuns(
            Authentication authentication,
            @RequestParam(required = false) UUID areaComumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime horaInicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime horaFim,
            Model model
    ) {
        var currentUser = support.authenticatedUser(authentication);
        var areas = reservaUseCases.listarAreasComuns();

        model.addAttribute("pageTitle", "Areas Comuns");
        model.addAttribute("areasComuns", support.mapContent(areas, support::toAreaComumMap));
        model.addAttribute("filtroAreaComumId", areaComumId);
        model.addAttribute("filtroData", data);
        model.addAttribute("filtroHoraInicio", horaInicio);
        model.addAttribute("filtroHoraFim", horaFim);

        if (areaComumId != null && data != null && horaInicio != null && horaFim != null) {
            boolean disponivel = reservaUseCases.consultarDisponibilidade(
                    currentUser, areaComumId, data, horaInicio, horaFim
            );
            model.addAttribute("resultadoDisponibilidade", disponivel);
        }

        return "morador/areas-comuns/lista";
    }

    @Operation(summary = "Lista as reservas do proprio morador", tags = "20 - Morador Web - Reservas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagina de reservas do morador renderizada com sucesso."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado.")
    })
    @GetMapping({"/reservas", "/reservas/"})
    @Transactional(readOnly = true)
    public String listarMinhasReservas(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID areaComumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Model model
    ) {
        var currentUser = support.authenticatedUser(authentication);
        var reservas = reservaUseCases.listarMinhasReservas(
                currentUser, status, areaComumId, data, support.pageRequest(page, size)
        );
        var areas = reservaUseCases.listarAreasComuns();

        model.addAttribute("pageTitle", "Minhas Reservas");
        model.addAttribute("reservas", support.mapContent(reservas.content(), support::toReservaMap));
        model.addAttribute("reservasPage", support.pageMetadata(reservas));
        model.addAttribute("areasComuns", support.mapContent(areas, support::toAreaComumMap));
        model.addAttribute("filtroStatus", status);
        model.addAttribute("filtroAreaComumId", areaComumId);
        model.addAttribute("filtroData", data);
        return "morador/reservas/lista";
    }

    @Operation(summary = "Exibe o detalhe de uma reserva do morador", tags = "20 - Morador Web - Reservas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagina de detalhe da reserva renderizada com sucesso."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado."),
            @ApiResponse(responseCode = "404", description = "Reserva nao encontrada para o morador.")
    })
    @GetMapping("/reservas/{reservaId}")
    @Transactional(readOnly = true)
    public String detalharReserva(
            Authentication authentication,
            @PathVariable UUID reservaId,
            Model model
    ) {
        var currentUser = support.authenticatedUser(authentication);
        var reserva = reservaUseCases.buscarMinhaReserva(currentUser, reservaId);

        model.addAttribute("pageTitle", "Detalhes da Reserva");
        model.addAttribute("reserva", support.toReservaMap(reserva));
        return "morador/reservas/detalhe";
    }
}