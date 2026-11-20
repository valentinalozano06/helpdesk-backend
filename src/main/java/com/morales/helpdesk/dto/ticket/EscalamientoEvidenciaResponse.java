package com.morales.helpdesk.dto.ticket;
import java.time.LocalDateTime;
public record EscalamientoEvidenciaResponse(Long id, String nombreArchivo, String tipoMime, Long tamano, String usuario, LocalDateTime fechaCreacion) {}
