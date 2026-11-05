package com.morales.helpdesk.service.impl;

import com.morales.helpdesk.dto.auth.LoginRequest;
import com.morales.helpdesk.dto.auth.LoginResponse;
import com.morales.helpdesk.dto.auth.RegisterRequest;
import com.morales.helpdesk.exception.ResourceNotFoundException;
import com.morales.helpdesk.model.Rol;
import com.morales.helpdesk.model.Usuario;
import com.morales.helpdesk.repository.UsuarioRepository;
import com.morales.helpdesk.security.JwtUtil;
import com.morales.helpdesk.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autenticacion y registro (CE0232). El login delega la verificacion de
 * credenciales al AuthenticationManager de Spring Security (que a su vez
 * usa BCrypt sobre el hash almacenado), y solo tras esa validacion en
 * backend se emite el JWT.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final com.morales.helpdesk.service.UsuarioService usuarioService;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            log.warn("Intento de login fallido para {}", request.email());
            throw ex;
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getId(), usuario.getRol().getNombre());
        log.info("Login exitoso: usuario {} ({})", usuario.getEmail(), usuario.getRol().getNombre());

        return new LoginResponse(
                token, "Bearer", usuario.getId(), usuario.getNombreCompleto(),
                usuario.getRol().getNombre(), jwtUtil.getExpirationMs());
    }

    @Override
    @Transactional
    public LoginResponse registrar(RegisterRequest request) {
        Usuario usuario = usuarioService.crear(request);
        Rol rol = usuario.getRol();

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getId(), rol.getNombre());
        return new LoginResponse(token, "Bearer", usuario.getId(), usuario.getNombreCompleto(),
                rol.getNombre(), jwtUtil.getExpirationMs());
    }
}
