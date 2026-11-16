package com.morales.helpdesk.dto.ticket;

import org.springframework.core.io.Resource;

/**
 * Empaqueta los metadatos del adjunto junto con el contenido del
 * archivo para que el controlador construya la respuesta HTTP.
 */
public record ContenidoAdjunto(AdjuntoResponse adjunto, Resource contenido) {
}
