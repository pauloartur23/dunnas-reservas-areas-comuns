package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.form.CadastrarAreaComumForm;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.form.NegarReservaForm;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminReservaWebController {

    private final ReservaUseCases reservaUseCases;
    private final WebControllerSupport support;

    public AdminReservaWebController(ReservaUseCases reservaUseCases, WebControllerSupport support) {
        this.reservaUseCases = reservaUseCases;
        this.support = support;
    }

    @ModelAttribute("cadastrarAreaComumForm")
    public CadastrarAreaComumForm cadastrarAreaComumForm() {
        return new CadastrarAreaComumForm();
    }

    @ModelAttribute("negarReservaForm")
    public NegarReservaForm negarReservaForm() {
        return new NegarReservaForm();
    }

    @Operation(summary = "Lista e permite cadastrar ou editar areas comuns", tags = "22 - Admin Web - Reservas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagina de areas comuns renderizada com sucesso."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado."),
            @ApiResponse(responseCode = "404", description = "Area comum informada para edicao nao encontrada.")
    })
    @GetMapping({"/areas-comuns", "/areas-comuns/"})
    @Transactional(readOnly = true)
    public String listarAreasComuns(
            Authentication authentication,
            @RequestParam(required = false) UUID areaComumId,
            Model model
    ) {
        var areas = reservaUseCases.listarAreasComuns();

        model.addAttribute("pageTitle", "Areas Comuns");
        model.addAttribute("areasComuns", support.mapContent(areas, support::toAreaComumMap));

        if (areaComumId != null) {
            var currentUser = support.authenticatedUser(authentication);
            var area = reservaUseCases.buscarAreaComumPorId(currentUser, areaComumId);

            var form = new CadastrarAreaComumForm();
            form.setNome(area.getNome());
            form.setDescricao(area.getDescricao());
            form.setHorarioAbertura(area.getHorarioAbertura());
            form.setHorarioFechamento(area.getHorarioFechamento());
            form.setDiasFuncionamento(
                    area.getDiasFuncionamento().stream().map(Enum::name).collect(Collectors.toSet())
            );

            model.addAttribute("areaComumEdicao", support.toAreaComumMap(area));
            model.addAttribute("cadastrarAreaComumForm", form);
        }

        return "admin/areas-comuns/lista";
    }

    @Operation(summary = "Lista todas as reservas do condominio", tags = "22 - Admin Web - Reservas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagina de reservas do administrador renderizada com sucesso."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado.")
    })
    @GetMapping({"/reservas", "/reservas/"})
    @Transactional(readOnly = true)
    public String listarReservas(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID areaComumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Model model
    ) {
        var currentUser = support.authenticatedUser(authentication);
        var reservas = reservaUseCases.listarReservasParaAdmin(
                currentUser, status, areaComumId, data, support.pageRequest(page, size)
        );
        var areas = reservaUseCases.listarAreasComuns();

        model.addAttribute("pageTitle", "Reservas");
        model.addAttribute("reservas", support.mapContent(reservas.content(), support::toReservaMap));
        model.addAttribute("reservasPage", support.pageMetadata(reservas));
        model.addAttribute("areasComuns", support.mapContent(areas, support::toAreaComumMap));
        model.addAttribute("filtroStatus", status);
        model.addAttribute("filtroAreaComumId", areaComumId);
        model.addAttribute("filtroData", data);
        return "admin/reservas/lista";
    }
}