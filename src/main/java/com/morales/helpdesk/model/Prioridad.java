package com.morales.helpdesk.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prioridades")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Prioridad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre; // baja, media, alta, critica

    @Column(nullable = false)
    private Integer nivel; // 1..4, usado por el clasificador
}
