package com.morales.helpdesk.repository;

import com.morales.helpdesk.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
