package com.morales.helpdesk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Auditoria de cambios sobre un ticket (RNF de trazabilidad).
 * Cada modificacion relevante queda registrada con usuario, fecha y valores.
 */
@Entity
@Table(name = "historial_cambios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorialCambio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "campo_modificado", nullable = false, length = 60)
    private String campoModificado;

    @Column(name = "valor_anterior")
    private String valorAnterior;

    @Column(name = "valor_nuevo")
    private String valorNuevo;

    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaCambio = LocalDateTime.now();
}
