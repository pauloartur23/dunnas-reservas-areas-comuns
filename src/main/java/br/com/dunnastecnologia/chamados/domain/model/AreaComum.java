package br.com.dunnastecnologia.chamados.domain.model;

import br.com.dunnastecnologia.chamados.domain.validation.ValidationLimits;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "areas_comuns")
@Getter
@Setter
public class AreaComum {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = ValidationLimits.DEFAULT_TEXT_MAX_LENGTH)
    private String nome;

    @Column(length = ValidationLimits.DEFAULT_TEXT_MAX_LENGTH)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativa = Boolean.TRUE;

    @Column
    private LocalTime horarioAbertura;

    @Column
    private LocalTime horarioFechamento;

    @Column(name = "duracao_maxima_minutos")
    private Integer duracaoMaximaMinutos;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "area_comum_dia_funcionamento", joinColumns = @JoinColumn(name = "area_comum_id"))
    @Column(name = "dia_semana", length = 15)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> diasFuncionamento = new HashSet<>();
}