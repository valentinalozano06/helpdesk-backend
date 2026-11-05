package com.morales.helpdesk.service;

import com.morales.helpdesk.model.Notificacion;
import com.morales.helpdesk.model.Usuario;

import java.util.List;

public interface NotificacionService {
    /** Envia la notificacion por todos los canales activos (in-app + email). */
    void notificarEvento(Usuario destinatario, String tipoEvento, String mensaje);
    void notificarEvento(Usuario destinatario, String tipoEvento, String mensaje, Long ticketId);
    List<Notificacion> listarPorUsuario(Long usuarioId);
}
