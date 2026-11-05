package com.morales.helpdesk.service;

import com.morales.helpdesk.dto.common.CatalogoResponse;

import java.util.List;

public interface CatalogoService {
    List<CatalogoResponse> categorias();
    List<CatalogoResponse> prioridades();
    List<CatalogoResponse> estados();
    List<CatalogoResponse> areas();
}
