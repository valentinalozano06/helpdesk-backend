package com.morales.helpdesk.repository;

import com.morales.helpdesk.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    List<Comentario> findByTicketIdOrderByFechaCreacionAsc(Long ticketId);

    void deleteByTicketId(Long ticketId);
}
