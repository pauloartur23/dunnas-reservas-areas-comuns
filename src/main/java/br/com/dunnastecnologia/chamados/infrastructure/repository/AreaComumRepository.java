package br.com.dunnastecnologia.chamados.infrastructure.repository;

import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AreaComumRepository extends JpaRepository<AreaComum, UUID> {

    boolean existsByIdAndAtivaTrue(UUID id);

    /**
     * RN adicionada: nome de area comum e unico ignorando maiuscula/minuscula.
     * Usado no cadastro (nao ha area nenhuma pra ignorar).
     */
    boolean existsByNomeIgnoreCase(String nome);

    /**
     * Mesma checagem, mas ignorando a propria area (usado na edicao, pra area nao
     * colidir com o proprio nome dela mesma quando o admin salva sem mudar o nome).
     */
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AreaComum a where a.id = :id")
    Optional<AreaComum> buscarComLockParaDecisao(@Param("id") UUID id);
}