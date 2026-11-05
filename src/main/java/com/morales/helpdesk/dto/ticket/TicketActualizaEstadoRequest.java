package com.morales.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotNull;

public record TicketActualizaEstadoRequest(@NotNull @jakarta.validation.constraints.Positive Long estadoId) {
}
