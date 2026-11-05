package com.morales.helpdesk.dto.common;

import java.time.LocalDateTime;
import java.util.List;

/** Formato unico de error de la API, usado por el GlobalExceptionHandler. */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> detalles
) {
}
