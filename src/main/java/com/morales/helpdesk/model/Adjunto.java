package com.morales.helpdesk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Evidencia adjunta a un ticket (imagenes o PDF). El archivo fisico se
 * almacena en el directorio de uploads del servidor; esta entidad guarda
 * sus metadatos y la ruta interna de almacenamiento.
 */
@Entity
@Table(name = "adjuntos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Adjunto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(nullable = false)
    private Long tamano;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
