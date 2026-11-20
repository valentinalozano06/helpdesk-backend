package com.morales.helpdesk.service;

import com.morales.helpdesk.dto.ticket.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface EscalamientoService {
    EscalamientoResponse solicitar(Long ticketId, EscalamientoRequest request, Authentication auth);
    List<EscalamientoResponse> pendientes(Authentication auth);
    EscalamientoResponse ultimoDeTicket(Long ticketId, Authentication auth);
    List<EscalamientoEvidenciaResponse> subirEvidencia(Long escalamientoId, List<MultipartFile> archivos, Authentication auth);
    ContenidoEscalamientoEvidencia descargarEvidencia(Long evidenciaId, Authentication auth);
    EscalamientoResponse decidir(Long escalamientoId, EscalamientoDecisionRequest request, Authentication auth);
}
