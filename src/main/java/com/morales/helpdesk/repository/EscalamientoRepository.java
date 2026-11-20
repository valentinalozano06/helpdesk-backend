package com.morales.helpdesk.repository;

import com.morales.helpdesk.model.Escalamiento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EscalamientoRepository extends JpaRepository<Escalamiento, Long> {
    List<Escalamiento> findByTicketIdOrderByFechaCreacionAscIdAsc(Long ticketId);
    Optional<Escalamiento> findFirstByTicketIdOrderByFechaCreacionDescIdDesc(Long ticketId);
    @EntityGraph(attributePaths = {"ticket", "ticket.solicitante", "ticket.asignadoA", "ticket.categoria", "ticket.prioridad", "ticket.estado", "tecnico", "decisionPor"})
    List<Escalamiento> findByEstadoOrderByFechaCreacionAscIdAsc(String estado);
    @EntityGraph(attributePaths = {"ticket", "ticket.solicitante", "ticket.asignadoA", "ticket.categoria", "ticket.prioridad", "ticket.estado", "tecnico", "decisionPor"})
    Optional<Escalamiento> findWithTicketById(Long id);
}
