package com.morales.helpdesk.controller;

import com.morales.helpdesk.dto.ticket.*;
import com.morales.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST del ciclo de vida del ticket (CE0233). Cada accion valida
 * el rol en backend con @PreAuthorize, ademas de las reglas globales
 * de SecurityConfig.
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @PreAuthorize("hasRole('SOLICITANTE')")
    public ResponseEntity<TicketResponse> crear(@Valid @RequestBody TicketRequest request, Authentication auth) {
        return ResponseEntity.status(201).body(ticketService.crear(request, auth));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> listar(Authentication auth) {
        return ResponseEntity.ok(ticketService.listarParaUsuarioActual(auth));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> obtener(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(ticketService.obtenerPorId(id, auth));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN', 'JEFATURA')")
    public ResponseEntity<TicketResponse> actualizarEstado(
            @PathVariable Long id, @Valid @RequestBody TicketActualizaEstadoRequest request, Authentication auth) {
        return ResponseEntity.ok(ticketService.actualizarEstado(id, request, auth));
    }

    @PatchMapping("/{id}/prioridad")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFATURA')")
    public ResponseEntity<TicketResponse> actualizarPrioridad(
            @PathVariable Long id, @Valid @RequestBody TicketActualizaPrioridadRequest request, Authentication auth) {
        return ResponseEntity.ok(ticketService.actualizarPrioridad(id, request, auth));
    }

    @PatchMapping("/{id}/asignar")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFATURA')")
    public ResponseEntity<TicketResponse> asignar(
            @PathVariable Long id, @Valid @RequestBody TicketAsignaRequest request, Authentication auth) {
        return ResponseEntity.ok(ticketService.asignar(id, request, auth));
    }

    @PostMapping("/{id}/comentarios")
    public ResponseEntity<ComentarioResponse> comentar(
            @PathVariable Long id, @Valid @RequestBody ComentarioRequest request, Authentication auth) {
        return ResponseEntity.status(201).body(ticketService.comentar(id, request, auth));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialCambioResponse>> historial(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(ticketService.listarHistorial(id, auth));
    }

    @GetMapping("/{id}/comentarios")
    public ResponseEntity<List<ComentarioResponse>> comentarios(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(ticketService.listarComentarios(id, auth));
    }
}
