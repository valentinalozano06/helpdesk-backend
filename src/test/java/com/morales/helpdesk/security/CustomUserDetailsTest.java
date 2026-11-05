package com.morales.helpdesk.security;

import com.morales.helpdesk.model.Rol;
import com.morales.helpdesk.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomUserDetailsTest {

    @Test
    void traduceRolesDeBaseDeDatosAAuthoritiesDeSpring() {
        Map<String, String> rolesEsperados = Map.of(
                "rol_solicitante", "ROLE_SOLICITANTE",
                "rol_tecnico", "ROLE_TECNICO",
                "rol_jefatura", "ROLE_JEFATURA",
                "rol_admin", "ROLE_ADMIN"
        );

        rolesEsperados.forEach((rolBaseDatos, authorityEsperada) -> {
            Usuario usuario = Usuario.builder()
                    .id(1L)
                    .nombres("Usuario")
                    .apellidos("Prueba")
                    .email("prueba@example.com")
                    .passwordHash("hash")
                    .rol(Rol.builder().nombre(rolBaseDatos).build())
                    .build();

            Collection<? extends GrantedAuthority> authorities = new CustomUserDetails(usuario).getAuthorities();

            assertTrue(
                    authorities.stream().anyMatch(authority -> authorityEsperada.equals(authority.getAuthority())),
                    () -> rolBaseDatos + " debe mapear a " + authorityEsperada
            );
        });
    }
}