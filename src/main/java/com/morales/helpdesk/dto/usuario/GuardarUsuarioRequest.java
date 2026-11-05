package com.morales.helpdesk.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuardarUsuarioRequest(
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres")
        String password,
        @NotBlank @jakarta.validation.constraints.Pattern(regexp = "rol_admin|rol_jefatura|rol_tecnico|rol_solicitante") String rol,
        @jakarta.validation.constraints.Positive Long areaId
) {
    public GuardarUsuarioRequest {
        email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
        nombres = nombres == null ? null : nombres.trim();
        apellidos = apellidos == null ? null : apellidos.trim();
        password = password == null || password.isBlank() ? null : password;
    }
}
