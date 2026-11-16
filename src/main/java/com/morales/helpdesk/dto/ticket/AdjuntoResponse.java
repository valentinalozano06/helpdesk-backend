package com.morales.helpdesk.dto.ticket;

import java.time.LocalDateTime;

public record AdjuntoResponse(
        Long id,
        String nombreArchivo,
        String tipoMime,
        Long tamano,
        String usuario,
        LocalDateTime fechaCreacion
) {
}
