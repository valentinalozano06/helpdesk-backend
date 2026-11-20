package com.morales.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EscalamientoRequest(
    @NotBlank @Size(max = 120) String motivo,
    @NotBlank @Size(max = 4000) String diagnostico,
    @NotBlank @Size(max = 4000) String accionesIntentadas
) {}
