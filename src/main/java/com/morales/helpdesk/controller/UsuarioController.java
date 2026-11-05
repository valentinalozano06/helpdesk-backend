package com.morales.helpdesk.controller;

import com.morales.helpdesk.dto.auth.RegisterRequest;
import com.morales.helpdesk.dto.common.CatalogoResponse;
import com.morales.helpdesk.dto.usuario.CambiarEstadoUsuarioRequest;
import com.morales.helpdesk.dto.usuario.GuardarUsuarioRequest;
import com.morales.helpdesk.dto.usuario.UsuarioResponse;
import com.morales.helpdesk.exception.BusinessException;
import com.morales.helpdesk.mapper.UsuarioMapper;
import com.morales.helpdesk.security.CustomUserDetails;
import com.morales.helpdesk.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion de usuarios (pantalla "Usuarios" del panel admin).
 * Acceso restringido a ADMIN / JEFATURA, reforzado ademas por
 * SecurityConfig a nivel de ruta /api/usuarios/**.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    /** Lista de tecnicos activos, usada para asignar tickets. */
    @GetMapping("/tecnicos")
    public List<CatalogoResponse> tecnicos() {
        return usuarioService.listarTecnicos().stream()
                .map(u -> new CatalogoResponse(u.getId(), u.getNombreCompleto()))
                .toList();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFATURA')")
    public List<UsuarioResponse> listar() {
        return usuarioService.listarTodos().stream().map(usuarioMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFATURA')")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioMapper.toResponse(usuarioService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(usuarioMapper.toResponse(usuarioService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody GuardarUsuarioRequest request, Authentication auth) {
        if (auth.getPrincipal() instanceof CustomUserDetails user && user.getId().equals(id)
                && !"rol_admin".equals(request.rol())) {
            throw new BusinessException("No puedes quitar el rol administrador de tu propia cuenta");
        }
        return ResponseEntity.ok(usuarioMapper.toResponse(usuarioService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> cambiarEstado(
            @PathVariable Long id, @Valid @RequestBody CambiarEstadoUsuarioRequest request, Authentication auth) {
        protegerPropiaCuenta(id, auth);
        return ResponseEntity.ok(usuarioMapper.toResponse(usuarioService.cambiarEstado(id, request.estado())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        protegerPropiaCuenta(id, auth);
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private void protegerPropiaCuenta(Long id, Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails user && user.getId().equals(id)) {
            throw new BusinessException("No puedes desactivar o eliminar tu propia cuenta");
        }
    }
}
