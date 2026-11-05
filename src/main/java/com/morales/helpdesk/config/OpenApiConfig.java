package com.morales.helpdesk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI helpdeskApi() {
        return new OpenAPI().info(new Info().title("Helpdesk Morales API").version("1.0")
                .description("API PostgreSQL. Solo el login es publico. Usuarios: ADMIN escribe, JEFATURA consulta. "
                    + "Tickets: solicitantes propios, tecnicos asignados, supervisores vista global. "
                    + "Errores: 400 validacion, 401 autenticacion, 403 permisos, 404 inexistente, 422 negocio."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
    @Bean
    public org.springdoc.core.customizers.OpenApiCustomizer contratosHttp() {
        return api -> {
            api.getComponents().getSchemas().putAll(io.swagger.v3.core.converter.ModelConverters.getInstance()
                    .read(com.morales.helpdesk.dto.common.ApiErrorResponse.class));
            api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                if (path.equals("/api/auth/login")) operation.setSecurity(java.util.List.of());
                boolean created = method == io.swagger.v3.oas.models.PathItem.HttpMethod.POST
                        && (path.equals("/api/usuarios") || path.equals("/api/tickets")
                            || path.equals("/api/auth/registro") || path.endsWith("/comentarios"));
                if (created) {
                    var response = operation.getResponses().remove("200");
                    if (response != null) operation.getResponses().addApiResponse("201", response.description("Creado"));
                }
                if (method == io.swagger.v3.oas.models.PathItem.HttpMethod.DELETE) {
                    operation.getResponses().remove("200");
                    operation.getResponses().addApiResponse("204", new io.swagger.v3.oas.models.responses.ApiResponse()
                            .description("Eliminado, o desactivado si tiene historial. Sin cuerpo."));
                }
                java.util.Map.of("400", "Datos invalidos", "401", "Autenticacion requerida o credenciales invalidas",
                        "403", "Sin permisos", "404", "Recurso no encontrado", "422", "Regla de negocio o datos incompatibles")
                    .forEach((code, description) -> operation.getResponses().addApiResponse(code,
                        new io.swagger.v3.oas.models.responses.ApiResponse().description(description)
                            .content(new io.swagger.v3.oas.models.media.Content().addMediaType("application/json",
                                new io.swagger.v3.oas.models.media.MediaType().schema(new io.swagger.v3.oas.models.media.Schema<>()
                                    .$ref("#/components/schemas/ApiErrorResponse"))))));
            }));
        };
    }
}
