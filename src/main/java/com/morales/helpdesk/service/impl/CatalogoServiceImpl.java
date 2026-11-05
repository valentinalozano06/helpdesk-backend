package com.morales.helpdesk.service.impl;

import com.morales.helpdesk.dto.common.CatalogoResponse;
import com.morales.helpdesk.repository.AreaRepository;
import com.morales.helpdesk.repository.CategoriaRepository;
import com.morales.helpdesk.repository.EstadoTicketRepository;
import com.morales.helpdesk.repository.PrioridadRepository;
import com.morales.helpdesk.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private final CategoriaRepository categoriaRepository;
    private final PrioridadRepository prioridadRepository;
    private final EstadoTicketRepository estadoTicketRepository;
    private final AreaRepository areaRepository;

    @Override
    public List<CatalogoResponse> categorias() {
        return categoriaRepository.findAll().stream()
                .map(c -> new CatalogoResponse(c.getId(), c.getNombre())).toList();
    }

    @Override
    public List<CatalogoResponse> prioridades() {
        return prioridadRepository.findAll().stream()
                .map(p -> new CatalogoResponse(p.getId(), p.getNombre())).toList();
    }

    @Override
    public List<CatalogoResponse> estados() {
        return estadoTicketRepository.findAll().stream()
                .map(e -> new CatalogoResponse(e.getId(), e.getNombre())).toList();
    }

    @Override
    public List<CatalogoResponse> areas() {
        return areaRepository.findAll().stream()
                .map(a -> new CatalogoResponse(a.getId(), a.getNombre())).toList();
    }
}
