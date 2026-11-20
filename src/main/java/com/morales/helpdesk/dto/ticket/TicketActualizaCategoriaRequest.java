package com.morales.helpdesk.dto.ticket;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
public record TicketActualizaCategoriaRequest(@NotNull @Positive Long categoriaId) {}
