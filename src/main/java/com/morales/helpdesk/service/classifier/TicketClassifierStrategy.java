package com.morales.helpdesk.service.classifier;

/**
 * Patron Strategy: define el contrato para sugerir la prioridad de un
 * ticket a partir de su asunto y descripcion. Permite intercambiar el
 * algoritmo (reglas locales vs. modelo de IA externo) sin tocar el
 * servicio de negocio que lo consume.
 */
public interface TicketClassifierStrategy {
    ClasificacionSugerida clasificar(String asunto, String descripcion, String categoria);
}
