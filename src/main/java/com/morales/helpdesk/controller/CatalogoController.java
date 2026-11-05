package com.morales.helpdesk.controller;

import com.morales.helpdesk.dto.common.CatalogoResponse;
import com.morales.helpdesk.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/categorias")
    public List<CatalogoResponse> categorias() { return catalogoService.categorias(); }

    @GetMapping("/prioridades")
    public List<CatalogoResponse> prioridades() { return catalogoService.prioridades(); }

    @GetMapping("/estados")
    public List<CatalogoResponse> estados() { return catalogoService.estados(); }

    @GetMapping("/areas")
    public List<CatalogoResponse> areas() { return catalogoService.areas(); }
}
