package com.morales.helpdesk.repository;

import com.morales.helpdesk.model.Adjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdjuntoRepository extends JpaRepository<Adjunto, Long> {

    List<Adjunto> findByTicketIdOrderByFechaCreacionAscIdAsc(Long ticketId);

    long countByTicketId(Long ticketId);

    void deleteByTicketId(Long ticketId);
}
