package com.morales.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotNull;

public record TicketActualizaPrioridadRequest(@NotNull @jakarta.validation.constraints.Positive Long prioridadId) {
}