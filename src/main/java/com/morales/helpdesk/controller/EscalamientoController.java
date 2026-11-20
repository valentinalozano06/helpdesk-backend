package com.morales.helpdesk.controller;

import com.morales.helpdesk.dto.ticket.*;
import com.morales.helpdesk.service.EscalamientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class EscalamientoController {
    private final EscalamientoService escalamientoService;

    @PostMapping("/api/tickets/{id}/escalamientos")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<EscalamientoResponse> solicitar(@PathVariable Long id, @Valid @RequestBody EscalamientoRequest request, Authentication auth) {
        return ResponseEntity.status(201).body(escalamientoService.solicitar(id, request, auth));
    }

    @GetMapping("/api/escalamientos/pendientes")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('JEFATURA','ADMIN')")
    public List<EscalamientoResponse> pendientes(Authentication auth) { return escalamientoService.pendientes(auth); }

    @GetMapping("/api/tickets/{ticketId}/escalamientos/ultimo")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('TECNICO','JEFATURA','ADMIN')")
    public ResponseEntity<EscalamientoResponse> ultimo(@PathVariable Long ticketId, Authentication auth) {
        EscalamientoResponse item = escalamientoService.ultimoDeTicket(ticketId, auth);
        return item == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(item);
    }

    @PostMapping("/api/escalamientos/{id}/evidencias")
    public ResponseEntity<List<EscalamientoEvidenciaResponse>> subir(@PathVariable Long id, @RequestParam("archivos") List<MultipartFile> archivos, Authentication auth) {
        return ResponseEntity.status(201).body(escalamientoService.subirEvidencia(id, archivos, auth));
    }

    @GetMapping("/api/escalamientos/evidencias/{id}/contenido")
    public ResponseEntity<Resource> descargar(@PathVariable Long id, Authentication auth) {
        ContenidoEscalamientoEvidencia contenido = escalamientoService.descargarEvidencia(id, auth);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contenido.evidencia().tipoMime()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + contenido.evidencia().nombreArchivo().replace("\"", "") + "\"")
            .body(contenido.contenido());
    }

    @PostMapping("/api/escalamientos/{id}/decision")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('JEFATURA')")
    public EscalamientoResponse decidir(@PathVariable Long id, @Valid @RequestBody EscalamientoDecisionRequest request, Authentication auth) {
        return escalamientoService.decidir(id, request, auth);
    }
}
