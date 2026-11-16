package com.morales.helpdesk.service;

import com.morales.helpdesk.dto.ticket.AdjuntoResponse;
import com.morales.helpdesk.dto.ticket.ContenidoAdjunto;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdjuntoService {
    void eliminar(Long id, Authentication auth);

    List<AdjuntoResponse> subir(Long ticketId, List<MultipartFile> archivos, Authentication auth);

    List<AdjuntoResponse> listar(Long ticketId, Authentication auth);

    ContenidoAdjunto descargar(Long id, Authentication auth);
}
