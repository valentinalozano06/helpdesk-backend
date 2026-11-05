package com.morales.helpdesk.service;

import com.morales.helpdesk.dto.ticket.*;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface TicketService {
    TicketResponse crear(TicketRequest request, Authentication auth);
    TicketResponse obtenerPorId(Long id, Authentication auth);
    List<TicketResponse> listarParaUsuarioActual(Authentication auth);
    TicketResponse actualizarEstado(Long id, TicketActualizaEstadoRequest request, Authentication auth);
    TicketResponse actualizarPrioridad(Long id, TicketActualizaPrioridadRequest request, Authentication auth);
    TicketResponse asignar(Long id, TicketAsignaRequest request, Authentication auth);
    ComentarioResponse comentar(Long id, ComentarioRequest request, Authentication auth);
    List<ComentarioResponse> listarComentarios(Long id, Authentication auth);
    List<HistorialCambioResponse> listarHistorial(Long id, Authentication auth);
}
