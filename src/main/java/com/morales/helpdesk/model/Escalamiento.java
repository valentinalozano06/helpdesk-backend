package com.morales.helpdesk.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "escalamientos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Escalamiento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tecnico_id", nullable = false)
    private Usuario tecnico;
    @Column(nullable = false, length = 120) private String motivo;
    @Column(nullable = false, columnDefinition = "TEXT") private String diagnostico;
    @Column(name = "acciones_intentadas", nullable = false, columnDefinition = "TEXT") private String accionesIntentadas;
    @Column(nullable = false, length = 20) @Builder.Default private String estado = "pendiente";
    @Column(columnDefinition = "TEXT") private String decision;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "decision_por_id") private Usuario decisionPor;
    @Column(name = "fecha_creacion", nullable = false, updatable = false) @Builder.Default private LocalDateTime fechaCreacion = LocalDateTime.now();
    @Column(name = "fecha_decision") private LocalDateTime fechaDecision;
}
