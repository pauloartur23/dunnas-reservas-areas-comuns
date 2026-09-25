package br.com.dunnastecnologia.chamados.infrastructure.repository;

import br.com.dunnastecnologia.chamados.domain.model.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador, UUID> {

    /**
     * Busca um administrador pelo e-mail para autenticacao e validacoes cadastrais.
     */
    @Query("""
            select a
            from Administrador a
            where lower(a.email) = lower(:email)
              and a.ativo = true
            """)
    Optional<Administrador> findByEmail(@Param("email") String email);

    boolean existsByIdAndAtivoTrue(UUID id);

    Optional<Administrador> findByIdAndAtivoTrue(UUID id);
}