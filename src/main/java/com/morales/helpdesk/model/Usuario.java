package com.morales.helpdesk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Hash BCrypt de la contrasena. Nunca se expone en los DTO de salida. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * EAGER a proposito: el rol se necesita para autorizacion en CADA
     * peticion (JwtAuthenticationFilter, CustomUserDetails.getAuthorities),
     * fuera de la sesion de Hibernate original. Con LAZY, Spring Security
     * intenta leerlo despues de cerrada la sesion y lanza
     * LazyInitializationException.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "activo"; // activo / inactivo

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "ultima_asignacion")
    private LocalDateTime ultimaAsignacion;

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}