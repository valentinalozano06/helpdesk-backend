package com.morales.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRequest(
        @NotBlank @Size(max = 200) String asunto,
        @NotBlank String descripcion,
        @NotNull @jakarta.validation.constraints.Positive Long categoriaId,
        // La prioridad es opcional. Si el solicitante envia una prioridad,
        // se ignora y el sistema la sugiere automaticamente; TI la puede ajustar.
        Long prioridadId
) {
}
