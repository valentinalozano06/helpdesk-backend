package com.morales.helpdesk.controller;
import com.morales.helpdesk.model.Notificacion;
import com.morales.helpdesk.repository.NotificacionRepository;
import com.morales.helpdesk.exception.ResourceNotFoundException;
import com.morales.helpdesk.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
@RestController @RequestMapping("/api/notificaciones") @RequiredArgsConstructor
public class NotificacionController {
    private final NotificacionRepository repo;
    public record Aviso(Long id,String tipoEvento,String mensaje,boolean leida,LocalDateTime fechaCreacion,Long ticketId) {}
    public record Bandeja(List<Aviso> items,long noLeidas) {}
    private Long usuario(Authentication auth) { return ((CustomUserDetails)auth.getPrincipal()).getId(); }
    private Aviso dto(Notificacion n) { return new Aviso(n.getId(),n.getTipoEvento(),n.getMensaje(),n.getLeida(),n.getFechaCreacion(),n.getTicketId()); }
    @GetMapping @Transactional(readOnly=true)
    public Bandeja listar(Authentication auth) { Long id=usuario(auth); return new Bandeja(repo.findTop50ByUsuarioIdOrderByFechaCreacionDescIdDesc(id).stream().map(this::dto).toList(),repo.countByUsuarioIdAndLeidaFalse(id)); }
    @PatchMapping("/{id}/leida") @Transactional
    public Aviso leer(@PathVariable Long id,Authentication auth) {
        Notificacion n=repo.findByIdAndUsuarioId(id,usuario(auth)).orElseThrow(()->new ResourceNotFoundException("Notificación no encontrada"));
        n.setLeida(true); return dto(repo.save(n));
    }
    @PostMapping("/leer-todas") @Transactional
    public Bandeja leerTodas(Authentication auth) { repo.leerTodas(usuario(auth)); return listar(auth); }
}
