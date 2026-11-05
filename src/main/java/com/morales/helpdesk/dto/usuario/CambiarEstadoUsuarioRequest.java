package com.morales.helpdesk.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarEstadoUsuarioRequest(
        @NotBlank
        @Pattern(regexp = "activo|inactivo", message = "El estado debe ser 'activo' o 'inactivo'")
        String estado
) {
}
