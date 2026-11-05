package com.morales.helpdesk.mapper;

import com.morales.helpdesk.dto.ticket.ComentarioResponse;
import com.morales.helpdesk.dto.ticket.HistorialCambioResponse;
import com.morales.helpdesk.model.HistorialCambio;
import com.morales.helpdesk.dto.ticket.TicketResponse;
import com.morales.helpdesk.model.Comentario;
import com.morales.helpdesk.model.Ticket;
import com.morales.helpdesk.model.Usuario;
import org.springframework.stereotype.Component;

/**
 * Traduce entidades JPA a DTO de salida. Mantiene la capa web desacoplada
 * del modelo de persistencia (patron DTO / Mapper).
 */
@Component
public class TicketMapper {

    public HistorialCambioResponse toResponse(HistorialCambio cambio) {
        return new HistorialCambioResponse(cambio.getId(), cambio.getUsuario().getNombreCompleto(),
                cambio.getCampoModificado(), cambio.getValorAnterior(), cambio.getValorNuevo(),
                cambio.getFechaCambio());
    }

    public TicketResponse toResponse(Ticket t) {
        Usuario asignado = t.getAsignadoA();
        return new TicketResponse(
                t.getId(),
                t.getCodigoUnico(),
                t.getAsunto(),
                t.getDescripcion(),
                t.getCategoria().getNombre(),
                t.getPrioridad().getNombre(),
                t.getEstado().getNombre(),
                t.getSolicitante().getNombreCompleto(),
                asignado != null ? asignado.getNombreCompleto() : null,
                t.getFechaCreacion(),
                t.getFechaUltimaActualizacion(),
                t.getSolicitante().getId(),
                asignado != null ? asignado.getId() : null
        );
    }

    public ComentarioResponse toResponse(Comentario c) {
        return new ComentarioResponse(
                c.getId(),
                c.getContenido(),
                c.getUsuario().getNombreCompleto(),
                c.getFechaCreacion()
        );
    }
}
