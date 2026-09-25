package br.com.dunnastecnologia.chamados.infrastructure.repository;

import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AreaComumRepository extends JpaRepository<AreaComum, UUID> {

    boolean existsByIdAndAtivaTrue(UUID id);
}