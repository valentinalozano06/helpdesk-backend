package com.morales.helpdesk.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estados_ticket")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EstadoTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre; // abierto, en_progreso, resuelto, cerrado

    @Column(nullable = false)
    private Integer orden;
}
