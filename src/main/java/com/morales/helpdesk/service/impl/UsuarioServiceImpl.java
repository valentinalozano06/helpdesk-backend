package com.morales.helpdesk.service.impl;

import com.morales.helpdesk.dto.auth.RegisterRequest;
import com.morales.helpdesk.dto.usuario.GuardarUsuarioRequest;
import com.morales.helpdesk.exception.BusinessException;
import com.morales.helpdesk.exception.ResourceNotFoundException;
import com.morales.helpdesk.model.Area;
import com.morales.helpdesk.model.Rol;
import com.morales.helpdesk.model.Usuario;
import com.morales.helpdesk.repository.AreaRepository;
import com.morales.helpdesk.repository.RolRepository;
import com.morales.helpdesk.repository.UsuarioRepository;
import com.morales.helpdesk.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioServiceImpl.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Usuario> listarTecnicos() {
        Long rolTecnicoId = rolRepository.findByNombre("rol_tecnico")
                .orElseThrow(() -> new ResourceNotFoundException("Rol rol_tecnico no configurado"))
                .getId();
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRol().getId().equals(rolTecnicoId) && "activo".equals(u.getEstado()))
                .toList();
    }

    @Override
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    @Override
    @Transactional
    public Usuario crear(RegisterRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("Ya existe un usuario registrado con ese correo");
        }
        Rol rol = rolRepository.findByNombre(request.rol())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no valido: " + request.rol()));

        Usuario usuario = Usuario.builder()
                .nombres(request.nombres())
                .apellidos(request.apellidos())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .rol(rol)
                .estado("activo")
                .build();

        if (request.areaId() != null) {
            Area area = areaRepository.findById(request.areaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Area no encontrada"));
            usuario.setArea(area);
        }

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario creado por administrador: {} ({})", usuario.getEmail(), rol.getNombre());
        return usuario;
    }

    @Override
    @Transactional
    public Usuario actualizar(Long id, GuardarUsuarioRequest request) {
        Usuario usuario = obtenerPorId(id);
        usuarioRepository.findByEmailIgnoreCase(request.email())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new BusinessException("Ya existe un usuario registrado con ese correo");
                });

        Rol rol = rolRepository.findByNombre(request.rol())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no valido: " + request.rol()));

        usuario.setNombres(request.nombres().trim());
        usuario.setApellidos(request.apellidos().trim());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setRol(rol);

        if (request.areaId() != null) {
            Area area = areaRepository.findById(request.areaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Area no encontrada"));
            usuario.setArea(area);
        } else {
            usuario.setArea(null);
        }

        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        usuarioRepository.save(usuario);
        log.info("Usuario actualizado por administrador: {} ({})", usuario.getEmail(), rol.getNombre());
        return usuario;
    }

    @Override
    @Transactional
    public Usuario cambiarEstado(Long id, String nuevoEstado) {
        Usuario usuario = obtenerPorId(id);
        usuario.setEstado(nuevoEstado);
        usuarioRepository.save(usuario);
        log.info("Usuario {} cambio a estado {}", usuario.getEmail(), nuevoEstado);
        return usuario;
    }

    @Override
    @Transactional
    public boolean eliminar(Long id) {
        Usuario usuario = obtenerPorId(id);
        if (tieneDatosRelacionados(id)) {
            usuario.setEstado("inactivo");
            usuarioRepository.save(usuario);
            log.info("Usuario {} desactivado por tener datos relacionados", usuario.getEmail());
            return false;
        }

        usuarioRepository.delete(usuario);
        log.info("Usuario eliminado por administrador: {}", usuario.getEmail());
        return true;
    }

    private boolean tieneDatosRelacionados(Long id) {
        Integer referencias = jdbcTemplate.queryForObject("""
                SELECT
                  (SELECT count(*) FROM tickets WHERE solicitante_id = ? OR asignado_a_id = ?) +
                  (SELECT count(*) FROM comentarios WHERE usuario_id = ?) +
                  (SELECT count(*) FROM historial_cambios WHERE usuario_id = ?) +
                  (SELECT count(*) FROM notificaciones WHERE usuario_id = ?)
                """, Integer.class, id, id, id, id, id);
        return referencias != null && referencias > 0;
    }
}
