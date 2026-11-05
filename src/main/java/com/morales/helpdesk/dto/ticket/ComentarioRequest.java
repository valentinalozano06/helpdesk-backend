package com.morales.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;

public record ComentarioRequest(@NotBlank @jakarta.validation.constraints.Size(max = 2000) String contenido) {
}
