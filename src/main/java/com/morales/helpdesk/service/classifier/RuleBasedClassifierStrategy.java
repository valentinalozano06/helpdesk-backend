package com.morales.helpdesk.service.classifier;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Estrategia de respaldo (fallback): clasificacion por palabras clave.
 * Se usa cuando el proveedor de IA no esta configurado o falla, de modo
 * que la funcionalidad de clasificacion nunca deja de estar disponible.
 */
@Component
public class RuleBasedClassifierStrategy implements TicketClassifierStrategy {

    private static final List<String> PALABRAS_CRITICAS =
            List.of("caido", "caída", "no funciona ningun", "servidor down", "sin servicio", "produccion detenida");
    private static final List<String> PALABRAS_ALTAS =
            List.of("no puedo acceder", "bloqueado", "urgente", "no responde", "perdida de datos");
    private static final List<String> PALABRAS_MEDIAS =
            List.of("lento", "intermitente", "error al", "falla");

    @Override
    public ClasificacionSugerida clasificar(String asunto, String descripcion, String categoria) {
        String texto = ((asunto == null ? "" : asunto) + " " + (descripcion == null ? "" : descripcion))
                .toLowerCase(Locale.ROOT);

        if (contieneAlguna(texto, PALABRAS_CRITICAS)) {
            return new ClasificacionSugerida("critica", "Se detectaron senales de interrupcion total del servicio", "reglas");
        }
        if (contieneAlguna(texto, PALABRAS_ALTAS)) {
            return new ClasificacionSugerida("alta", "El texto indica bloqueo o urgencia para el usuario", "reglas");
        }
        if (contieneAlguna(texto, PALABRAS_MEDIAS)) {
            return new ClasificacionSugerida("media", "El texto describe una falla parcial o intermitente", "reglas");
        }
        return new ClasificacionSugerida("baja", "No se detectaron palabras clave de urgencia", "reglas");
    }

    private boolean contieneAlguna(String texto, List<String> palabras) {
        return palabras.stream().anyMatch(texto::contains);
    }
}
