package br.com.dunnastecnologia.chamados.domain.model;

import br.com.dunnastecnologia.chamados.domain.validation.ValidationLimits;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import lombok.Getter;
import lombok.Setter;

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
}