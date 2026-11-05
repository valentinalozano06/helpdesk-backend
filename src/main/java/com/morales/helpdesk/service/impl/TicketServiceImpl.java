package com.morales.helpdesk.service.impl;

import com.morales.helpdesk.dto.ticket.*;
import com.morales.helpdesk.exception.BusinessException;
import com.morales.helpdesk.exception.ResourceNotFoundException;
import com.morales.helpdesk.mapper.TicketMapper;
import com.morales.helpdesk.model.*;
import com.morales.helpdesk.repository.*;
import com.morales.helpdesk.security.CustomUserDetails;
import com.morales.helpdesk.service.NotificacionService;
import com.morales.helpdesk.service.TicketService;
import com.morales.helpdesk.service.classifier.ClasificacionSugerida;
import com.morales.helpdesk.service.classifier.ClasificadorTicketService;
import com.morales.helpdesk.util.TicketCodeBuilder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Reglas de negocio del ciclo de vida del ticket (RF-03 a RF-09).
 * Estructurado en capas: este servicio no accede a HTTP ni a JPA
 * directamente mas alla de los repositorios inyectados, y delega
 * clasificacion, auditoria y notificacion a colaboradores dedicados.
 */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

    private final TicketRepository ticketRepository;
    private final AjustesRepository ajustesRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final CategoriaRepository categoriaRepository;
    private final PrioridadRepository prioridadRepository;
    private final EstadoTicketRepository estadoTicketRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComentarioRepository comentarioRepository;
    private final HistorialCambioRepository historialCambioRepository;

    private final ClasificadorTicketService clasificadorTicketService;
    private final NotificacionService notificacionService;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional
    public TicketResponse crear(TicketRequest request, Authentication auth) {
        Usuario solicitante = usuarioActual(auth);

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));

        Prioridad prioridad = resolverPrioridad(request, categoria, solicitante);

        EstadoTicket estadoInicial = estadoTicketRepository.findByNombre("abierto")
                .orElseThrow(() -> new ResourceNotFoundException("Estado 'abierto' no configurado"));

        ajustesRepository.bloquearReparto();
        Ticket ticket = Ticket.builder()
                .codigoUnico(generarCodigoUnico())
                .asunto(request.asunto())
                .descripcion(request.descripcion())
                .categoria(categoria)
                .prioridad(prioridad)
                .estado(estadoInicial)
                .solicitante(solicitante)
                .build();

        ticket = ticketRepository.save(ticket);
        if (ajustesRepository.asignacionAutomatica()) {
            Usuario tecnico = usuarioRepository.siguienteTecnico().orElse(null);
            if (tecnico != null) {
                ticket.setAsignadoA(tecnico);
                tecnico.setUltimaAsignacion(java.time.LocalDateTime.now());
                usuarioRepository.save(tecnico);
                ticketRepository.saveAndFlush(ticket);
                registrarHistorial(ticket, solicitante, "asignado_a", "sin asignar", tecnico.getNombreCompleto());
                notificacionService.notificarEvento(tecnico, "ticket_asignado", "Se te asignó el ticket " + ticket.getCodigoUnico() + ": " + ticket.getAsunto(), ticket.getId());
            }
        }
        log.info("Ticket {} creado por {}", ticket.getCodigoUnico(), solicitante.getEmail());

        notificacionService.notificarEvento(solicitante, "ticket_creado",
                "Se registro tu ticket " + ticket.getCodigoUnico() + ": " + ticket.getAsunto(), ticket.getId());

        return ticketMapper.toResponse(ticket);
    }

    private Prioridad resolverPrioridad(TicketRequest request, Categoria categoria, Usuario solicitante) {
        if (!"rol_solicitante".equals(solicitante.getRol().getNombre()) && request.prioridadId() != null) {
            return prioridadRepository.findById(request.prioridadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Prioridad no encontrada"));
        }
        // CE0234: sin prioridad explicita, se consulta el componente inteligente.
        ClasificacionSugerida sugerencia = clasificadorTicketService.sugerirPrioridad(
                request.asunto(), request.descripcion(), categoria.getNombre());
        log.info("Prioridad sugerida por [{}]: {} ({})", sugerencia.origen(), sugerencia.prioridad(), sugerencia.motivo());
        return prioridadRepository.findByNombre(sugerencia.prioridad())
                .orElseGet(() -> prioridadRepository.findByNombre("media")
                        .orElseThrow(() -> new ResourceNotFoundException("Catalogo de prioridades no configurado")));
    }

    private String generarCodigoUnico() {
        LocalDate hoy = LocalDate.now();
        TicketCodeBuilder builder = TicketCodeBuilder.nuevo().conFecha(hoy);
        Long correlativo = jdbc.queryForObject("""
            INSERT INTO ticket_correlativos(fecha,ultimo)
            VALUES (?, (SELECT COALESCE(MAX(split_part(codigo_unico,'-',3)::bigint),0)+1
                        FROM tickets WHERE codigo_unico ~ ?))
            ON CONFLICT(fecha) DO UPDATE SET ultimo=ticket_correlativos.ultimo+1
            RETURNING ultimo
            """, Long.class, java.sql.Date.valueOf(hoy), "^" + builder.prefijoDelDia() + "[0-9]+$");
        return builder.conCorrelativo(correlativo).build();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse obtenerPorId(Long id, Authentication auth) {
        Ticket ticket = buscarTicket(id); validarLectura(ticket, usuarioActual(auth));
        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listarParaUsuarioActual(Authentication auth) {
        Usuario usuario = usuarioActual(auth);
        String rol = usuario.getRol().getNombre();

        List<Ticket> tickets = switch (rol) {
            case "rol_solicitante" -> ticketRepository.findBySolicitanteId(usuario.getId());
            case "rol_tecnico" -> ticketRepository.findByAsignadoAIdOrSolicitanteId(usuario.getId(), usuario.getId());
            default -> ticketRepository.findAll(); // jefatura / admin: vista global
        };
        return tickets.stream().map(ticketMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public TicketResponse actualizarEstado(Long id, TicketActualizaEstadoRequest request, Authentication auth) {
        ajustesRepository.bloquearReparto();
        Ticket ticket = buscarTicket(id);
        Usuario actor = usuarioActual(auth);
        validarGestion(ticket, actor);
        EstadoTicket nuevoEstado = estadoTicketRepository.findById(request.estadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Estado no encontrado"));

        String anterior = ticket.getEstado().getNombre();
        if ("rol_tecnico".equals(actor.getRol().getNombre()) && !anterior.equals(nuevoEstado.getNombre())) {
            boolean iniciar = "en_progreso".equals(nuevoEstado.getNombre()) && java.util.Set.of("abierto","en_espera").contains(anterior);
            boolean resolver = "resuelto".equals(nuevoEstado.getNombre()) && "en_progreso".equals(anterior);
            if (!iniciar && !resolver) throw new BusinessException("El técnico debe iniciar el ticket antes de resolverlo; no puede reabrirlo ni cerrarlo.");
        }
        if (anterior.equals(nuevoEstado.getNombre())) {
            return ticketMapper.toResponse(ticket);
        }

        ticket.setEstado(nuevoEstado);
        ticketRepository.saveAndFlush(ticket);

        registrarHistorial(ticket, usuarioActual(auth), "estado", anterior, nuevoEstado.getNombre());
        log.info("Ticket {} cambio de estado: {} -> {}", ticket.getCodigoUnico(), anterior, nuevoEstado.getNombre());

        notificacionService.notificarEvento(ticket.getSolicitante(), "ticket_" + nuevoEstado.getNombre(),
                "Tu ticket " + ticket.getCodigoUnico() + " cambio a estado: " + nuevoEstado.getNombre(), ticket.getId());

        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse actualizarPrioridad(Long id, TicketActualizaPrioridadRequest request, Authentication auth) {
        validarSupervisor(usuarioActual(auth));
        Ticket ticket = buscarTicket(id);
        Prioridad nuevaPrioridad = prioridadRepository.findById(request.prioridadId())
                .orElseThrow(() -> new ResourceNotFoundException("Prioridad no encontrada"));

        String anterior = ticket.getPrioridad().getNombre();
        if (anterior.equals(nuevaPrioridad.getNombre())) {
            return ticketMapper.toResponse(ticket);
        }

        ticket.setPrioridad(nuevaPrioridad);
        ticketRepository.saveAndFlush(ticket);

        registrarHistorial(ticket, usuarioActual(auth), "prioridad", anterior, nuevaPrioridad.getNombre());
        log.info("Ticket {} cambio de prioridad: {} -> {}", ticket.getCodigoUnico(), anterior, nuevaPrioridad.getNombre());

        notificacionService.notificarEvento(ticket.getSolicitante(), "ticket_prioridad_actualizada",
                "Tu ticket " + ticket.getCodigoUnico() + " fue priorizado como: " + nuevaPrioridad.getNombre(), ticket.getId());

        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse asignar(Long id, TicketAsignaRequest request, Authentication auth) {
        validarSupervisor(usuarioActual(auth));
        ajustesRepository.bloquearReparto();
        Ticket ticket = buscarTicket(id);
        Usuario tecnico = usuarioRepository.findById(request.tecnicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Tecnico no encontrado"));

        if (!"rol_tecnico".equals(tecnico.getRol().getNombre()) || !"activo".equals(tecnico.getEstado())) {
            throw new BusinessException("El usuario indicado no tiene rol de tecnico");
        }

        if (ticket.getAsignadoA() != null && ticket.getAsignadoA().getId().equals(tecnico.getId())) return ticketMapper.toResponse(ticket);
        tecnico.setUltimaAsignacion(java.time.LocalDateTime.now()); usuarioRepository.save(tecnico);
        String anterior = ticket.getAsignadoA() != null ? ticket.getAsignadoA().getNombreCompleto() : "sin asignar";
        ticket.setAsignadoA(tecnico);
        ticketRepository.saveAndFlush(ticket);

        registrarHistorial(ticket, usuarioActual(auth), "asignado_a", anterior, tecnico.getNombreCompleto());
        log.info("Ticket {} asignado a {}", ticket.getCodigoUnico(), tecnico.getEmail());

        notificacionService.notificarEvento(tecnico, "ticket_asignado",
                "Se te asigno el ticket " + ticket.getCodigoUnico() + ": " + ticket.getAsunto(), ticket.getId());

        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional
    public ComentarioResponse comentar(Long id, ComentarioRequest request, Authentication auth) {
        Ticket ticket = buscarTicket(id);
        Usuario autor = usuarioActual(auth);
        validarLectura(ticket, autor);

        Comentario comentario = Comentario.builder()
                .ticket(ticket)
                .usuario(autor)
                .contenido(request.contenido().trim())
                .build();
        comentario = comentarioRepository.save(comentario);

        log.info("Nuevo comentario en ticket {} por {}", ticket.getCodigoUnico(), autor.getEmail());

        Usuario aNotificar = autor.getId().equals(ticket.getSolicitante().getId())
                ? ticket.getAsignadoA() : ticket.getSolicitante();
        if (aNotificar != null) {
            notificacionService.notificarEvento(aNotificar, "ticket_comentado",
                    "Nuevo comentario en el ticket " + ticket.getCodigoUnico(), ticket.getId());
        }

        return ticketMapper.toResponse(comentario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComentarioResponse> listarComentarios(Long id, Authentication auth) {
        validarLectura(buscarTicket(id), usuarioActual(auth));
        return comentarioRepository.findByTicketIdOrderByFechaCreacionAsc(id).stream()
                .map(ticketMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialCambioResponse> listarHistorial(Long id, Authentication auth) {
        validarLectura(buscarTicket(id), usuarioActual(auth));
        return historialCambioRepository.findByTicketIdOrderByFechaCambioDescIdDesc(id).stream()
                .map(ticketMapper::toResponse).toList();
    }

    private void registrarHistorial(Ticket ticket, Usuario usuario, String campo, String valorAnterior, String valorNuevo) {
        historialCambioRepository.save(HistorialCambio.builder()
                .ticket(ticket).usuario(usuario)
                .campoModificado(campo).valorAnterior(valorAnterior).valorNuevo(valorNuevo)
                .build());
    }

    private Ticket buscarTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado: " + id));
    }

    private void validarSupervisor(Usuario u) {
        if (!java.util.Set.of("rol_admin","rol_jefatura").contains(u.getRol().getNombre()))
            throw new org.springframework.security.access.AccessDeniedException("Solo supervisores");
    }
    private void validarLectura(Ticket t, Usuario u) {
        String rol = u.getRol().getNombre();
        if (java.util.Set.of("rol_admin","rol_jefatura").contains(rol) || t.getSolicitante().getId().equals(u.getId())) return;
        if ("rol_tecnico".equals(rol) && t.getAsignadoA()!=null && t.getAsignadoA().getId().equals(u.getId())) return;
        throw new org.springframework.security.access.AccessDeniedException("Ticket de otro usuario");
    }
    private void validarGestion(Ticket t, Usuario u) {
        if (java.util.Set.of("rol_admin","rol_jefatura").contains(u.getRol().getNombre())) return;
        if ("rol_tecnico".equals(u.getRol().getNombre()) && t.getAsignadoA()!=null && t.getAsignadoA().getId().equals(u.getId())) return;
        throw new org.springframework.security.access.AccessDeniedException("No es el técnico asignado");
    }
    private Usuario usuarioActual(Authentication auth) {
        Long id = ((CustomUserDetails) auth.getPrincipal()).getId();
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }
}
