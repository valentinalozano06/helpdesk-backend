package com.morales.helpdesk.repository;

import com.morales.helpdesk.model.EstadoTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoTicketRepository extends JpaRepository<EstadoTicket, Long> {
    Optional<EstadoTicket> findByNombre(String nombre);
}
