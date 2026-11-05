package com.morales.helpdesk.service.notification;

import com.morales.helpdesk.model.Usuario;

/**
 * Patron Strategy: cada canal de notificacion implementa esta interfaz.
 * El servicio de negocio no sabe (ni le importa) si la notificacion
 * termina siendo un correo, un registro en la app u otro canal futuro.
 */
public interface Notifier {
    void notificar(Usuario destinatario, String asunto, String mensaje);

    default void notificar(Usuario destinatario, String asunto, String mensaje, Long ticketId) {
        notificar(destinatario, asunto, mensaje);
    }

    /** Identifica el canal, usado por la fabrica para elegir la estrategia. */
    String tipo();
}
