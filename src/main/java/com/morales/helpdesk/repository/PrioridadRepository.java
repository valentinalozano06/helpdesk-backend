package com.morales.helpdesk.repository;

import com.morales.helpdesk.model.Prioridad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrioridadRepository extends JpaRepository<Prioridad, Long> {
    Optional<Prioridad> findByNombre(String nombre);
}
