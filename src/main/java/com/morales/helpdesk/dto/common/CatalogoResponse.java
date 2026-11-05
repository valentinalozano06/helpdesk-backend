package com.morales.helpdesk.dto.common;

/** DTO generico para catalogos de solo lectura (categorias, prioridades, estados, areas). */
public record CatalogoResponse(Long id, String nombre) {
}
