package com.morales.helpdesk.mapper;

import com.morales.helpdesk.dto.usuario.UsuarioResponse;
import com.morales.helpdesk.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {
    public UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNombres(),
                u.getApellidos(),
                u.getNombreCompleto(),
                u.getEmail(),
                u.getArea() != null ? u.getArea().getNombre() : null,
                u.getRol().getNombre(),
                u.getEstado()
        );
    }
}
