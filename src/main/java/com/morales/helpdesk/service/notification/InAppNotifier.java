package com.morales.helpdesk.service.notification;

import com.morales.helpdesk.model.Notificacion;
import com.morales.helpdesk.model.Usuario;
import com.morales.helpdesk.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Notificacion interna persistida en la tabla `notificaciones` (bandeja dentro del sistema). */
@Component
@RequiredArgsConstructor
public class InAppNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(InAppNotifier.class);
    private final NotificacionRepository notificacionRepository;

    @Override
    public void notificar(Usuario destinatario, String asunto, String mensaje) {
        notificar(destinatario, asunto, mensaje, null);
    }
    @Override
    public void notificar(Usuario destinatario, String asunto, String mensaje, Long ticketId) {
        Notificacion n = Notificacion.builder()
                .usuario(destinatario)
                .ticketId(ticketId)
                .tipoEvento(asunto)
                .mensaje(mensaje)
                .build();
        notificacionRepository.save(n);
        log.info("Notificacion interna registrada para usuario {} ({})", destinatario.getId(), asunto);
    }

    @Override
    public String tipo() {
        return "in_app";
    }
}
