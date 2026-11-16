package com.morales.helpdesk.controller;

import com.morales.helpdesk.dto.ticket.AdjuntoResponse;
import com.morales.helpdesk.dto.ticket.ContenidoAdjunto;
import com.morales.helpdesk.service.AdjuntoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * API REST de la evidencia adjunta por ticket. Los permisos se validan
 * en el servicio por usuario y ticket: quien puede ver el ticket puede
 * adjuntar y revisar su evidencia (solicitante, tecnico asignado,
 * jefatura o admin).
 */
@RestController
@RequiredArgsConstructor
public class AdjuntoController {

    private final AdjuntoService adjuntoService;

    @org.springframework.web.bind.annotation.DeleteMapping("/api/adjuntos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        adjuntoService.eliminar(id, auth);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/tickets/{id}/adjuntos")
    public ResponseEntity<List<AdjuntoResponse>> subir(
            @PathVariable Long id,
            @RequestParam("archivos") List<MultipartFile> archivos,
            Authentication auth) {
        return ResponseEntity.status(201).body(adjuntoService.subir(id, archivos, auth));
    }

    @GetMapping("/api/tickets/{id}/adjuntos")
    public ResponseEntity<List<AdjuntoResponse>> listar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(adjuntoService.listar(id, auth));
    }

    @GetMapping("/api/adjuntos/{id}/contenido")
    public ResponseEntity<Resource> descargar(@PathVariable Long id, Authentication auth) {
        ContenidoAdjunto contenido = adjuntoService.descargar(id, auth);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contenido.adjunto().tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + contenido.adjunto().nombreArchivo().replace("\"", "") + "\"")
                .body(contenido.contenido());
    }
}
