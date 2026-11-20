package com.morales.helpdesk.repository;

import com.morales.helpdesk.model.EscalamientoEvidencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EscalamientoEvidenciaRepository extends JpaRepository<EscalamientoEvidencia, Long> {
    @EntityGraph(attributePaths = {"usuario"})
    List<EscalamientoEvidencia> findByEscalamientoIdOrderByFechaCreacionAscIdAsc(Long escalamientoId);
    @EntityGraph(attributePaths = {"escalamiento", "escalamiento.ticket", "escalamiento.tecnico"})
    Optional<EscalamientoEvidencia> findWithEscalamientoById(Long id);
    long countByEscalamientoId(Long escalamientoId);
}
