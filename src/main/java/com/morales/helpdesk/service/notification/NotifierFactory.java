package com.morales.helpdesk.service.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Patron Factory: entrega la(s) estrategia(s) de notificacion adecuadas
 * segun el tipo de canal solicitado, sin que el resto del sistema
 * conozca las clases concretas (EmailNotifier, InAppNotifier, ...).
 */
@Component
@RequiredArgsConstructor
public class NotifierFactory {

    private final List<Notifier> notificadoresDisponibles;

    public Notifier obtener(String tipo) {
        Map<String, Notifier> indice = notificadoresDisponibles.stream()
                .collect(Collectors.toMap(Notifier::tipo, Function.identity()));
        Notifier n = indice.get(tipo);
        if (n == null) {
            throw new IllegalArgumentException("Canal de notificacion no soportado: " + tipo);
        }
        return n;
    }

    /** Notifica por todos los canales activos (in-app siempre + email si aplica). */
    public List<Notifier> obtenerCanalesPorDefecto() {
        return notificadoresDisponibles;
    }
}
