package br.com.dunnastecnologia.chamados.infrastructure.controller.web.form;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class CadastrarAreaComumForm {
    private String nome;
    private String descricao;
    private LocalTime horarioAbertura;
    private LocalTime horarioFechamento;
    private Integer duracaoMaximaMinutos;
    private Set<String> diasFuncionamento = new HashSet<>();
}