package com.morales.helpdesk.service.notification;

import com.morales.helpdesk.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Integracion con servicio externo de correo (CE0234) via SMTP
 * (JavaMailSender). El envio es asincrono para no bloquear el hilo
 * que atiende la peticion REST, y cualquier fallo del proveedor de
 * correo se registra pero no interrumpe el flujo del ticket.
 */
@Component
@RequiredArgsConstructor
public class EmailNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    private final JavaMailSender mailSender;

    @Value("${app.notifications.remitente}")
    private String remitente;

    @Value("${app.notifications.email-enabled}")
    private boolean habilitado;

    @Override
    @Async
    public void notificar(Usuario destinatario, String asunto, String mensaje) {
        if (!habilitado) {
            log.debug("Notificaciones por correo deshabilitadas; se omite envio a {}", destinatario.getEmail());
            return;
        }
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(remitente);
            email.setTo(destinatario.getEmail());
            email.setSubject(asunto);
            email.setText(mensaje);
            mailSender.send(email);
            log.info("Correo de notificacion enviado a {} ({})", destinatario.getEmail(), asunto);
        } catch (Exception ex) {
            // El fallo del servicio externo no debe romper la operacion de negocio.
            log.error("No se pudo enviar el correo a {}: {}", destinatario.getEmail(), ex.getMessage());
        }
    }

    @Override
    public String tipo() {
        return "email";
    }
}
