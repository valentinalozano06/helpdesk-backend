package com.morales.helpdesk.service.impl;

import com.morales.helpdesk.model.Notificacion;
import com.morales.helpdesk.model.Usuario;
import com.morales.helpdesk.repository.NotificacionRepository;
import com.morales.helpdesk.service.NotificacionService;
import com.morales.helpdesk.service.notification.Notifier;
import com.morales.helpdesk.service.notification.NotifierFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquesta el envio de notificaciones usando la fabrica de canales
 * (Factory + Strategy). No conoce si el canal es correo o interno.
 */
@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotifierFactory notifierFactory;
    private final NotificacionRepository notificacionRepository;

    @Override
    public void notificarEvento(Usuario destinatario, String tipoEvento, String mensaje) {
        notificarEvento(destinatario, tipoEvento, mensaje, null);
    }
    @Override
    public void notificarEvento(Usuario destinatario, String tipoEvento, String mensaje, Long ticketId) {
        for (Notifier canal : notifierFactory.obtenerCanalesPorDefecto()) {
            if ("in_app".equals(canal.tipo())) { canal.notificar(destinatario,tipoEvento,mensaje,ticketId); continue; }
            Usuario correo = Usuario.builder().id(destinatario.getId()).email(destinatario.getEmail()).build();
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() { canal.notificar(correo,tipoEvento,mensaje); }
                });
            } else canal.notificar(correo,tipoEvento,mensaje);
        }
    }

    @Override
    public List<Notificacion> listarPorUsuario(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }
}
