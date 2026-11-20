package com.morales.helpdesk.dto.ticket;

import java.time.LocalDateTime;
import java.util.List;

public record EscalamientoResponse(
    Long id, Long ticketId, String codigoUnico, String asunto, String solicitante,
    String tecnico, String categoria, String prioridad, String motivo,
    String diagnostico, String accionesIntentadas, String estado,
    String decision, String decisionPor, LocalDateTime fechaCreacion,
    LocalDateTime fechaDecision, List<EscalamientoEvidenciaResponse> evidencias
) {}
