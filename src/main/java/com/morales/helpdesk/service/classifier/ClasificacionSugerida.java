package com.morales.helpdesk.service.classifier;

/** Resultado del componente inteligente: prioridad sugerida y motivo (explicabilidad basica). */
public record ClasificacionSugerida(String prioridad, String motivo, String origen) {
}
