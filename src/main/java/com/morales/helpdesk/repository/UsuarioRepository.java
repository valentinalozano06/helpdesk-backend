package com.morales.helpdesk.repository;

import com.morales.helpdesk.model.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @Override
    @EntityGraph(attributePaths = {"area", "rol"})
    List<Usuario> findAll();

    @org.springframework.data.jpa.repository.Query(value = """
        SELECT u.* FROM usuarios u JOIN roles r ON r.id=u.rol_id
        LEFT JOIN tickets t ON t.asignado_a_id=u.id
        LEFT JOIN estados_ticket e ON e.id=t.estado_id
        WHERE r.nombre='rol_tecnico' AND u.estado='activo'
        GROUP BY u.id
        ORDER BY COUNT(t.id) FILTER (WHERE e.nombre NOT IN ('resuelto','cerrado')) ASC,
                 u.ultima_asignacion ASC NULLS FIRST, u.id ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<Usuario> siguienteTecnico();

    @Override
    @EntityGraph(attributePaths = {"area", "rol"})
    Optional<Usuario> findById(Long id);

    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
