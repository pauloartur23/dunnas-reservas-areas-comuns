package br.com.dunnastecnologia.chamados.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reserva_historico")
@Getter
@Setter
public class ReservaHistorico {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior")
    private StatusReserva statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false)
    private StatusReserva statusNovo;

    @Column(name = "autor_tipo", nullable = false)
    private String autorTipo;

    @Column(name = "autor_nome", nullable = false)
    private String autorNome;

    @Column(name = "observacao")
    private String observacao;

    @Column(name = "data_evento", nullable = false)
    private LocalDateTime dataEvento;
}