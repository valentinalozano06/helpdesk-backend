package com.morales.helpdesk.repository;

import com.morales.helpdesk.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);
    List<Notificacion> findTop50ByUsuarioIdOrderByFechaCreacionDescIdDesc(Long usuarioId);
    java.util.Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Notificacion n set n.leida=true where n.usuario.id=:usuarioId and n.leida=false")
    int leerTodas(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);
}
