package com.morales.helpdesk.controller;
import com.morales.helpdesk.repository.AjustesRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/ajustes") @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class AjustesController {
    private final AjustesRepository ajustes;
    public record AjustesDto(@NotNull Boolean asignacionAutomatica) {}
    @GetMapping public AjustesDto obtener() { return new AjustesDto(ajustes.asignacionAutomatica()); }
    @PutMapping @Transactional public AjustesDto guardar(@Valid @RequestBody AjustesDto datos) {
        ajustes.bloquearReparto(); ajustes.guardar(datos.asignacionAutomatica()); return obtener();
    }
}
