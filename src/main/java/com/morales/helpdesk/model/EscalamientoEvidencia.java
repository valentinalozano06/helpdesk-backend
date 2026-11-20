package com.morales.helpdesk.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "escalamiento_evidencias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EscalamientoEvidencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "escalamiento_id", nullable = false)
    private Escalamiento escalamiento;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(name = "nombre_archivo", nullable = false, length = 255) private String nombreArchivo;
    @Column(name = "tipo_mime", nullable = false, length = 100) private String tipoMime;
    @Column(nullable = false) private Long tamano;
    @Column(name = "ruta_archivo", nullable = false, length = 500) private String rutaArchivo;
    @Column(name = "fecha_creacion", nullable = false, updatable = false) @Builder.Default private LocalDateTime fechaCreacion = LocalDateTime.now();
}
