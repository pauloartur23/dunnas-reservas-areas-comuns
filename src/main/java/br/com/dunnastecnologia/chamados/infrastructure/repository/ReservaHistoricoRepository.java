package br.com.dunnastecnologia.chamados.infrastructure.repository;

import br.com.dunnastecnologia.chamados.domain.model.ReservaHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservaHistoricoRepository extends JpaRepository<ReservaHistorico, UUID> {

    List<ReservaHistorico> findByReservaIdOrderByDataEventoAsc(UUID reservaId);
}