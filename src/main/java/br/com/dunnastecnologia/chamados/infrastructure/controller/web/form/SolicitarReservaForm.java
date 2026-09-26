package br.com.dunnastecnologia.chamados.infrastructure.controller.web.form;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class SolicitarReservaForm {
    private UUID areaComumId;
    private LocalDate data;
    private LocalTime horaInicio;
    private LocalTime horaFim;
}