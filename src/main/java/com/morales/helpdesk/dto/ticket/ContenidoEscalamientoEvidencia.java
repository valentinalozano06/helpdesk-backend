package com.morales.helpdesk.dto.ticket;
import org.springframework.core.io.Resource;
public record ContenidoEscalamientoEvidencia(EscalamientoEvidenciaResponse evidencia, Resource contenido) {}
