package com.morales.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotNull;

public record TicketAsignaRequest(@NotNull @jakarta.validation.constraints.Positive Long tecnicoId) {
}
