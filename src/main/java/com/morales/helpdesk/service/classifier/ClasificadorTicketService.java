package com.morales.helpdesk.service.classifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Contexto del patron Strategy: decide en tiempo de ejecucion si usa el
 * clasificador de IA (cuando esta habilitado y configurado) o el
 * clasificador basado en reglas como respaldo automatico ante
 * cualquier fallo del servicio externo. Asi CE0233 (API/servicios) y
 * CE0234 (componente inteligente) quedan desacoplados del resto del
 * dominio: el TicketService solo conoce esta fachada.
 */
@Service
public class ClasificadorTicketService {

    private static final Logger log = LoggerFactory.getLogger(ClasificadorTicketService.class);

    private final AiClassifierStrategy estrategiaIa;
    private final RuleBasedClassifierStrategy estrategiaReglas;

    @Value("${app.ai.enabled}")
    private boolean iaHabilitada;

    public ClasificadorTicketService(AiClassifierStrategy estrategiaIa, RuleBasedClassifierStrategy estrategiaReglas) {
        this.estrategiaIa = estrategiaIa;
        this.estrategiaReglas = estrategiaReglas;
    }

    public ClasificacionSugerida sugerirPrioridad(String asunto, String descripcion, String categoria) {
        if (iaHabilitada) {
            try {
                return estrategiaIa.clasificar(asunto, descripcion, categoria);
            } catch (Exception ex) {
                log.warn("Fallo el clasificador de IA, se usa el respaldo por reglas: {}", ex.getMessage());
            }
        }
        return estrategiaReglas.clasificar(asunto, descripcion, categoria);
    }
}
