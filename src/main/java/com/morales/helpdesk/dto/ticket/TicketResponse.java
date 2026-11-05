package com.morales.helpdesk.dto.ticket;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String codigoUnico,
        String asunto,
        String descripcion,
        String categoria,
        String prioridad,
        String estado,
        String solicitante,
        String asignadoA,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaUltimaActualizacion,
        Long solicitanteId,
        Long asignadoAId
) {
}
