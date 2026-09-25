package br.com.dunnastecnologia.chamados.infrastructure.repository;

import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    @Query(value = """
            select *
            from fn_listar_reservas_do_morador(:moradorId, :status, :areaComumId, :data)
            """,
            countQuery = """
            select count(*)
            from fn_listar_reservas_do_morador(:moradorId, :status, :areaComumId, :data)
            """,
            nativeQuery = true)
    Page<Reserva> buscarParaMorador(
            @Param("moradorId") UUID moradorId,
            @Param("status") String status,
            @Param("areaComumId") UUID areaComumId,
            @Param("data") LocalDate data,
            Pageable pageable
    );

    @Query(value = """
            select *
            from fn_listar_reservas_para_admin(:status, :areaComumId, :data)
            """,
            countQuery = """
            select count(*)
            from fn_listar_reservas_para_admin(:status, :areaComumId, :data)
            """,
            nativeQuery = true)
    Page<Reserva> buscarParaAdmin(
            @Param("status") String status,
            @Param("areaComumId") UUID areaComumId,
            @Param("data") LocalDate data,
            Pageable pageable
    );

    @Query(value = """
            select *
            from fn_buscar_reserva_do_morador(:moradorId, :reservaId)
            """,
            nativeQuery = true)
    Optional<Reserva> findByIdAndMoradorId(
            @Param("moradorId") UUID moradorId,
            @Param("reservaId") UUID reservaId
    );

    @Query("""
            select case when count(r) > 0 then true else false end
            from Reserva r
            where r.areaComum.id = :areaComumId
              and r.status = 'APROVADA'
              and r.data = :data
              and r.horaInicio < :horaFim
              and r.horaFim > :horaInicio
            """)
    boolean existeReservaAprovadaConflitante(
            @Param("areaComumId") UUID areaComumId,
            @Param("data") LocalDate data,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFim") LocalTime horaFim
    );
}