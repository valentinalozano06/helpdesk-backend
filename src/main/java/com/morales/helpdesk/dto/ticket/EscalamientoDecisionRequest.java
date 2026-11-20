package com.morales.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EscalamientoDecisionRequest(
    @NotBlank @Pattern(regexp = "devolver|reasignar|no_resuelto") String accion,
    @NotBlank @Size(max = 4000) String decision,
    Long tecnicoId
) {}
