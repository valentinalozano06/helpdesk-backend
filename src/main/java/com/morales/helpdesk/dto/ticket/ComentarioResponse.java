package com.morales.helpdesk.dto.ticket;

import java.time.LocalDateTime;

public record ComentarioResponse(
        Long id,
        String contenido,
        String autor,
        LocalDateTime fechaCreacion,
        Long autorId,
        String rol
) {
}
