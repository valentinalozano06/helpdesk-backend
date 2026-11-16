package com.morales.helpdesk.repository;

import com.morales.helpdesk.model.HistorialCambio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface HistorialCambioRepository extends JpaRepository<HistorialCambio, Long> {
    @EntityGraph(attributePaths = "usuario")
    List<HistorialCambio> findByTicketIdOrderByFechaCambioDescIdDesc(Long ticketId);

    void deleteByTicketId(Long ticketId);
}
