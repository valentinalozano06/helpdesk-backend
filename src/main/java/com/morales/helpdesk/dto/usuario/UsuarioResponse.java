package com.morales.helpdesk.dto.usuario;

public record UsuarioResponse(
        Long id,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String email,
        String area,
        String rol,
        String estado
) {
}
