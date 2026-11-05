package com.morales.helpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Punto de entrada del sistema Help Desk - Municipalidad Distrital de Morales.
 * <p>
 * Arquitectura en capas: controller -> service -> repository -> model,
 * con transversales de seguridad (JWT), manejo de errores centralizado
 * y registro de eventos (AOP + auditoria en historial_cambios).
 */
@SpringBootApplication
@EnableAsync
public class HelpdeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(HelpdeskApplication.class, args);
    }
}
