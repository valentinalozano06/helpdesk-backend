package com.morales.helpdesk.controller;

import com.morales.helpdesk.dto.auth.LoginRequest;
import com.morales.helpdesk.dto.auth.LoginResponse;
import com.morales.helpdesk.dto.auth.RegisterRequest;
import com.morales.helpdesk.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @io.swagger.v3.oas.annotations.Operation(security = {}, summary = "Iniciar sesion")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Deprecated
    @io.swagger.v3.oas.annotations.Operation(deprecated = true, summary = "Registro heredado: usar POST /api/usuarios", description = "Solo ADMIN. Conserva la respuesta historica; no usar su token para reemplazar la sesion del administrador.")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/registro")
    public ResponseEntity<LoginResponse> registrar(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.registrar(request));
    }
}
