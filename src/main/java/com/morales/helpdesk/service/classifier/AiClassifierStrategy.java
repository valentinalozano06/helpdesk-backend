package com.morales.helpdesk.service.classifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Integracion con un servicio externo de IA (CE0234) para sugerir la
 * prioridad de un ticket a partir de lenguaje natural. Compatible con
 * cualquier API con contrato tipo "chat completions" (OpenAI y
 * proveedores equivalentes); se configura por variables de entorno.
 */
@Component
public class AiClassifierStrategy implements TicketClassifierStrategy {

    private static final Logger log = LoggerFactory.getLogger(AiClassifierStrategy.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.provider-url}")
    private String providerUrl;

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.model}")
    private String model;

    public AiClassifierStrategy(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public ClasificacionSugerida clasificar(String asunto, String descripcion, String categoria) {
        String prompt = """
                Eres un clasificador de tickets de soporte TI municipal.
                Categoria: %s
                Asunto: %s
                Descripcion: %s

                Responde SOLO con una de estas palabras: baja, media, alta, critica.
                """.formatted(categoria, asunto, descripcion);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", new Object[]{Map.of("role", "user", "content", prompt)},
                "temperature", 0.0
        );

        String respuesta = webClient.post()
                .uri(providerUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(6))
                .block();

        String prioridad = extraerPrioridad(respuesta);
        return new ClasificacionSugerida(prioridad, "Sugerido por el modelo de IA configurado (" + model + ")", "ia");
    }

    private String extraerPrioridad(String respuestaJson) {
        try {
            JsonNode raiz = objectMapper.readTree(respuestaJson);
            String contenido = raiz.path("choices").path(0).path("message").path("content").asText("").trim().toLowerCase();
            if (contenido.contains("critica")) return "critica";
            if (contenido.contains("alta")) return "alta";
            if (contenido.contains("media")) return "media";
            return "baja";
        } catch (Exception e) {
            log.warn("No se pudo interpretar la respuesta del proveedor de IA, se usara 'media' por defecto: {}", e.getMessage());
            return "media";
        }
    }
}
