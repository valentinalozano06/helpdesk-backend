package com.morales.helpdesk.dto.ticket;

import java.time.LocalDateTime;

public record HistorialCambioResponse(
        Long id, String usuario, String campoModificado,
        String valorAnterior, String valorNuevo, LocalDateTime fechaCambio) {
}
