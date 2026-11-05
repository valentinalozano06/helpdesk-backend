package com.morales.helpdesk.config;

import com.morales.helpdesk.model.Area;
import com.morales.helpdesk.model.Rol;
import com.morales.helpdesk.model.Usuario;
import com.morales.helpdesk.repository.AreaRepository;
import com.morales.helpdesk.repository.RolRepository;
import com.morales.helpdesk.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.bootstrap.legacy-enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class DatosInicialesRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosInicialesRunner.class);
    private static final String MARCADOR_USUARIOS_REALES = "usuarios_reales_20260929_aplicado";
    private static final String MARCADOR_PASSWORD_TEMPORAL = "usuarios_reales_password_20260930_aplicado";
    private static final String PASSWORD_TEMPORAL = System.getenv("HELPDESK_LEGACY_BOOTSTRAP_PASSWORD");
    private static final Set<String> EMAILS_REALES = Set.of(
            "jozefy67@gmail.com",
            "jerikaanthonella7@gmail.com",
            "nosoymiki123@protonmail.com",
            "lester.samuel.cordova.vivas@gmail.com",
            "sergiocachique19@gmail.com",
            "lopezreateguijj29@gmail.com"
    );

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (PASSWORD_TEMPORAL == null || PASSWORD_TEMPORAL.isBlank()) {
            throw new IllegalStateException("Configura HELPDESK_LEGACY_BOOTSTRAP_PASSWORD para activar el bootstrap legacy");
        }
        limpiarDatosDemoSiCorresponde();
        boolean forzarPasswordTemporal = passwordTemporalDebeAplicarse();

        asegurarUsuario("Jozef Aarón", "López Díaz", "jozefy67@gmail.com", PASSWORD_TEMPORAL, "rol_admin", "Tecnologias de la Informacion", forzarPasswordTemporal);
        asegurarUsuario("Jerika Anthonella", "Reyna Ramirez", "jerikaanthonella7@gmail.com", PASSWORD_TEMPORAL, "rol_jefatura", "Tecnologias de la Informacion", forzarPasswordTemporal);
        asegurarUsuario("Miguel Adrian", "Gonzales Rengifo", "nosoymiki123@protonmail.com", PASSWORD_TEMPORAL, "rol_tecnico", "Tecnologias de la Informacion", forzarPasswordTemporal);
        asegurarUsuario("Lester Samuel", "Cordova Vivas", "lester.samuel.cordova.vivas@gmail.com", PASSWORD_TEMPORAL, "rol_tecnico", "Tecnologias de la Informacion", forzarPasswordTemporal);
        asegurarUsuario("Sergio Andree", "Cachique Rodas", "sergiocachique19@gmail.com", PASSWORD_TEMPORAL, "rol_tecnico", "Tecnologias de la Informacion", forzarPasswordTemporal);
        asegurarUsuario("Josué Jesus", "López Reátegui", "lopezreateguijj29@gmail.com", PASSWORD_TEMPORAL, "rol_solicitante", "Administracion", forzarPasswordTemporal);

        if (forzarPasswordTemporal) {
            jdbcTemplate.update(
                    "INSERT INTO ajustes (clave, valor) VALUES (?, 'true') ON CONFLICT (clave) DO UPDATE SET valor = EXCLUDED.valor",
                    MARCADOR_PASSWORD_TEMPORAL
            );
        }
    }

    private void limpiarDatosDemoSiCorresponde() {
        Boolean aplicado = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM ajustes WHERE clave = ? AND valor = 'true')",
                Boolean.class,
                MARCADOR_USUARIOS_REALES
        );
        if (Boolean.TRUE.equals(aplicado)) {
            return;
        }

        log.info("Aplicando limpieza de usuarios y datos demo para usuarios reales");
        jdbcTemplate.update("DELETE FROM notificaciones");
        jdbcTemplate.update("DELETE FROM comentarios");
        jdbcTemplate.update("DELETE FROM historial_cambios");
        jdbcTemplate.update("DELETE FROM tickets");
        jdbcTemplate.update("DELETE FROM usuarios WHERE lower(email) NOT IN (?,?,?,?,?,?)", EMAILS_REALES.toArray());
        jdbcTemplate.update(
                "INSERT INTO ajustes (clave, valor) VALUES (?, 'true') ON CONFLICT (clave) DO UPDATE SET valor = EXCLUDED.valor",
                MARCADOR_USUARIOS_REALES
        );
    }

    private void asegurarUsuario(String nombres, String apellidos, String email, String password, String nombreRol, String nombreArea, boolean forzarPassword) {
        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new IllegalStateException("Rol no configurado: " + nombreRol));
        Area area = areaRepository.findByNombre(nombreArea)
                .orElseThrow(() -> new IllegalStateException("Area no configurada: " + nombreArea));

        Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(Usuario::new);
        boolean nuevo = usuario.getId() == null;
        boolean modificado = nuevo;

        if (!Objects.equals(usuario.getNombres(), nombres)) {
            usuario.setNombres(nombres);
            modificado = true;
        }
        if (!Objects.equals(usuario.getApellidos(), apellidos)) {
            usuario.setApellidos(apellidos);
            modificado = true;
        }
        if (!Objects.equals(usuario.getEmail(), email)) {
            usuario.setEmail(email);
            modificado = true;
        }
        if (nuevo || usuario.getPasswordHash() == null || forzarPassword) {
            usuario.setPasswordHash(passwordEncoder.encode(password));
            modificado = true;
        }
        if (usuario.getRol() == null || !Objects.equals(usuario.getRol().getId(), rol.getId())) {
            usuario.setRol(rol);
            modificado = true;
        }
        if (usuario.getArea() == null || !Objects.equals(usuario.getArea().getId(), area.getId())) {
            usuario.setArea(area);
            modificado = true;
        }
        if (usuario.getFechaCreacion() == null) {
            usuario.setFechaCreacion(LocalDateTime.now());
            modificado = true;
        }
        if (!"activo".equalsIgnoreCase(usuario.getEstado())) {
            usuario.setEstado("activo");
            modificado = true;
        }

        if (modificado) {
            usuarioRepository.save(usuario);
            log.info("Usuario base {}: {} ({}, {})", nuevo ? "creado" : "actualizado", email, nombreRol, nombreArea);
        }
    }

    private boolean passwordTemporalDebeAplicarse() {
        Boolean aplicado = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM ajustes WHERE clave = ? AND valor = 'true')",
                Boolean.class,
                MARCADOR_PASSWORD_TEMPORAL
        );
        return !Boolean.TRUE.equals(aplicado);
    }
}