package com.morales.helpdesk.repository;

import com.morales.helpdesk.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByCodigoUnico(String codigoUnico);

    List<Ticket> findBySolicitanteId(Long solicitanteId);

    List<Ticket> findByAsignadoAId(Long tecnicoId);
    List<Ticket> findByAsignadoAIdOrSolicitanteId(Long tecnicoId, Long solicitanteId);

    @Query("select count(t) from Ticket t where t.codigoUnico like concat(:prefijo, '%')")
    long contarPorPrefijoDeCodigo(@Param("prefijo") String prefijo);

    @Query("select t.estado.nombre as estado, count(t) as total from Ticket t group by t.estado.nombre")
    List<Object[]> contarPorEstado();
}
