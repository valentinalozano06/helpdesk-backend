package com.morales.helpdesk.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {
    public LoginRequest {
        email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
