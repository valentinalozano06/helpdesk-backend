package com.morales.helpdesk.dto.auth;

public record LoginResponse(
        String token,
        String tipo,
        Long usuarioId,
        String nombreCompleto,
        String rol,
        long expiraEnMs
) {
}
