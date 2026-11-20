package com.morales.helpdesk.service.impl;

import com.morales.helpdesk.dto.ticket.*;
import com.morales.helpdesk.exception.BusinessException;
import com.morales.helpdesk.exception.ResourceNotFoundException;
import com.morales.helpdesk.model.*;
import com.morales.helpdesk.repository.*;
import com.morales.helpdesk.security.CustomUserDetails;
import com.morales.helpdesk.service.EscalamientoService;
import com.morales.helpdesk.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EscalamientoServiceImpl implements EscalamientoService {
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoTicketRepository estadoRepository;
    private final EscalamientoRepository escalamientoRepository;
    private final EscalamientoEvidenciaRepository evidenciaRepository;
    private final HistorialCambioRepository historialRepository;
    private final NotificacionService notificacionService;

    @Value("${app.uploads.dir:uploads}") private String directorioAlmacen;
    private static final Set<String> EXTENSIONES = Set.of("png", "jpg", "jpeg", "pdf");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    @Override
    @Transactional
    public EscalamientoResponse solicitar(Long ticketId, EscalamientoRequest request, Authentication auth) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));
        Usuario actor = usuarioActual(auth);
        if (!"rol_tecnico".equals(actor.getRol().getNombre()) || ticket.getAsignadoA() == null || !ticket.getAsignadoA().getId().equals(actor.getId()))
            throw new AccessDeniedException("Solo el técnico asignado puede escalar este ticket");
        if (!"en_progreso".equals(ticket.getEstado().getNombre())) throw new BusinessException("Solo se puede escalar un ticket en atención");

        Escalamiento item = escalamientoRepository.save(Escalamiento.builder()
            .ticket(ticket).tecnico(actor).motivo(request.motivo().trim())
            .diagnostico(request.diagnostico().trim()).accionesIntentadas(request.accionesIntentadas().trim()).build());
        String anterior = ticket.getEstado().getNombre();
        ticket.setEstado(estado("requiere_revision"));
        ticketRepository.saveAndFlush(ticket);
        historialRepository.save(HistorialCambio.builder().ticket(ticket).usuario(actor).campoModificado("estado")
            .valorAnterior(anterior).valorNuevo("requiere_revision").build());
        historialRepository.save(HistorialCambio.builder().ticket(ticket).usuario(actor).campoModificado("escalamiento")
            .valorAnterior(null).valorNuevo("Escalado a Jefatura: " + item.getMotivo()).build());
        for (Usuario jefe : usuarioRepository.findByRolNombreAndEstadoOrderById("rol_jefatura", "activo")) {
            notificacionService.notificarEvento(jefe, "escalamiento_pendiente", "El ticket " + ticket.getCodigoUnico() + " requiere revisión de Jefatura", ticketId);
        }
        notificacionService.notificarEvento(ticket.getSolicitante(), "ticket_requiere_revision", "Tu ticket " + ticket.getCodigoUnico() + " fue escalado para revisión", ticketId);
        return response(item, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EscalamientoResponse> pendientes(Authentication auth) {
        Usuario actor = usuarioActual(auth);
        if (!Set.of("rol_jefatura", "rol_admin").contains(actor.getRol().getNombre())) throw new AccessDeniedException("Solo Jefatura puede consultar los escalamientos");
        return escalamientoRepository.findByEstadoOrderByFechaCreacionAscIdAsc("pendiente").stream()
            .map(item -> response(item, evidenciaRepository.findByEscalamientoIdOrderByFechaCreacionAscIdAsc(item.getId()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EscalamientoResponse ultimoDeTicket(Long ticketId, Authentication auth) {
        Usuario actor = usuarioActual(auth);
        Escalamiento item = escalamientoRepository.findFirstByTicketIdOrderByFechaCreacionDescIdDesc(ticketId).orElse(null);
        if (item == null) return null;
        validarAccesoInterno(item, actor);
        return response(item, evidenciaRepository.findByEscalamientoIdOrderByFechaCreacionAscIdAsc(item.getId()));
    }

    @Override
    @Transactional
    public List<EscalamientoEvidenciaResponse> subirEvidencia(Long escalamientoId, List<MultipartFile> archivos, Authentication auth) {
        Escalamiento item = buscarEscalamiento(escalamientoId);
        Usuario actor = usuarioActual(auth);
        validarAccesoInterno(item, actor);
        if (!"pendiente".equals(item.getEstado())) throw new BusinessException("El escalamiento ya fue revisado");
        if (archivos == null || archivos.isEmpty() || archivos.size() > 5) throw new BusinessException("Adjunta entre 1 y 5 archivos por carga");
        if (evidenciaRepository.countByEscalamientoId(escalamientoId) + archivos.size() > 10) throw new BusinessException("El escalamiento admite hasta 10 archivos");
        archivos.forEach(this::validarArchivo);
        Path carpeta = Paths.get(directorioAlmacen).resolve("escalamientos").resolve(escalamientoId.toString());
        try { Files.createDirectories(carpeta); } catch (IOException e) { throw new IllegalStateException("No se pudo preparar el almacén interno", e); }
        List<EscalamientoEvidencia> guardados = new ArrayList<>();
        for (MultipartFile archivo : archivos) guardados.add(guardar(item, actor, archivo, carpeta));
        return guardados.stream().map(this::response).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContenidoEscalamientoEvidencia descargarEvidencia(Long evidenciaId, Authentication auth) {
        EscalamientoEvidencia archivo = evidenciaRepository.findWithEscalamientoById(evidenciaId)
            .orElseThrow(() -> new ResourceNotFoundException("Evidencia interna no encontrada"));
        validarAccesoInterno(archivo.getEscalamiento(), usuarioActual(auth));
        Path ruta = Paths.get(archivo.getRutaArchivo()).normalize();
        if (!Files.isRegularFile(ruta)) throw new ResourceNotFoundException("El archivo de evidencia no está disponible");
        return new ContenidoEscalamientoEvidencia(response(archivo), new FileSystemResource(ruta));
    }

    @Override
    @Transactional
    public EscalamientoResponse decidir(Long escalamientoId, EscalamientoDecisionRequest request, Authentication auth) {
        Usuario jefe = usuarioActual(auth);
        if (!"rol_jefatura".equals(jefe.getRol().getNombre())) throw new AccessDeniedException("Solo Jefatura puede decidir un escalamiento");
        Escalamiento item = escalamientoRepository.findWithTicketById(escalamientoId)
            .orElseThrow(() -> new ResourceNotFoundException("Escalamiento no encontrado"));
        if (!"pendiente".equals(item.getEstado())) throw new BusinessException("Este escalamiento ya fue revisado");
        Ticket ticket = item.getTicket();
        if (!"requiere_revision".equals(ticket.getEstado().getNombre())) throw new BusinessException("El ticket ya no está en revisión");

        String estadoAnterior = ticket.getEstado().getNombre();
        item.setDecision(request.decision().trim()); item.setDecisionPor(jefe); item.setFechaDecision(LocalDateTime.now());
        if ("no_resuelto".equals(request.accion())) {
            item.setEstado("no_resuelto"); ticket.setEstado(estado("no_resuelto"));
        } else if ("devolver".equals(request.accion())) {
            item.setEstado("devuelto"); ticket.setEstado(estado("en_progreso"));
        } else {
            if (request.tecnicoId() == null) throw new BusinessException("Selecciona el técnico al que se reasignará el ticket");
            Usuario tecnico = usuarioRepository.findById(request.tecnicoId()).orElseThrow(() -> new ResourceNotFoundException("Técnico no encontrado"));
            if (!"rol_tecnico".equals(tecnico.getRol().getNombre()) || !"activo".equals(tecnico.getEstado())) throw new BusinessException("El usuario elegido no es un técnico activo");
            item.setEstado("devuelto"); ticket.setAsignadoA(tecnico); ticket.setEstado(estado("en_progreso"));
            historialRepository.save(HistorialCambio.builder().ticket(ticket).usuario(jefe).campoModificado("asignado_a")
                .valorAnterior(item.getTecnico().getNombreCompleto()).valorNuevo(tecnico.getNombreCompleto()).build());
            notificacionService.notificarEvento(tecnico, "ticket_asignado", "Se te asignó el ticket " + ticket.getCodigoUnico() + " tras revisión de Jefatura", ticket.getId());
        }
        escalamientoRepository.save(item);
        ticketRepository.saveAndFlush(ticket);
        historialRepository.save(HistorialCambio.builder().ticket(ticket).usuario(jefe).campoModificado("estado")
            .valorAnterior(estadoAnterior).valorNuevo(ticket.getEstado().getNombre()).build());
        notificacionService.notificarEvento(item.getTecnico(), "escalamiento_decidido", "Jefatura revisó el escalamiento de " + ticket.getCodigoUnico(), ticket.getId());
        notificacionService.notificarEvento(ticket.getSolicitante(), "ticket_" + ticket.getEstado().getNombre(), "Actualización de tu ticket " + ticket.getCodigoUnico(), ticket.getId());
        return response(item, evidenciaRepository.findByEscalamientoIdOrderByFechaCreacionAscIdAsc(item.getId()));
    }

    private Escalamiento buscarEscalamiento(Long id) { return escalamientoRepository.findWithTicketById(id).orElseThrow(() -> new ResourceNotFoundException("Escalamiento no encontrado")); }
    private EstadoTicket estado(String nombre) { return estadoRepository.findByNombre(nombre).orElseThrow(() -> new IllegalStateException("Estado no configurado: " + nombre)); }
    private Usuario usuarioActual(Authentication auth) { return usuarioRepository.findById(((CustomUserDetails) auth.getPrincipal()).getId()).orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado")); }
    private void validarAccesoInterno(Escalamiento item, Usuario usuario) {
        String rol = usuario.getRol().getNombre();
        if (Set.of("rol_jefatura", "rol_admin").contains(rol)) return;
        if ("rol_tecnico".equals(rol) && (item.getTecnico().getId().equals(usuario.getId()) ||
                (item.getTicket().getAsignadoA() != null && item.getTicket().getAsignadoA().getId().equals(usuario.getId())))) return;
        throw new AccessDeniedException("No tienes acceso a la información interna del escalamiento");
    }
    private EscalamientoResponse response(Escalamiento item, List<EscalamientoEvidencia> archivos) {
        Ticket t = item.getTicket();
        return new EscalamientoResponse(item.getId(), t.getId(), t.getCodigoUnico(), t.getAsunto(), t.getSolicitante().getNombreCompleto(), item.getTecnico().getNombreCompleto(), t.getCategoria().getNombre(), t.getPrioridad().getNombre(), item.getMotivo(), item.getDiagnostico(), item.getAccionesIntentadas(), item.getEstado(), item.getDecision(), item.getDecisionPor() == null ? null : item.getDecisionPor().getNombreCompleto(), item.getFechaCreacion(), item.getFechaDecision(), archivos.stream().map(this::response).toList());
    }
    private EscalamientoEvidenciaResponse response(EscalamientoEvidencia e) { return new EscalamientoEvidenciaResponse(e.getId(), e.getNombreArchivo(), e.getTipoMime(), e.getTamano(), e.getUsuario().getNombreCompleto(), e.getFechaCreacion()); }
    private EscalamientoEvidencia guardar(Escalamiento item, Usuario usuario, MultipartFile archivo, Path carpeta) {
        String nombre = StringUtils.cleanPath(Objects.requireNonNullElse(archivo.getOriginalFilename(), "evidencia").trim());
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).replace("\\", "_");
        String ext = extension(nombre); String almacen = UUID.randomUUID() + "." + ext; Path destino = carpeta.resolve(almacen);
        try (OutputStream salida = Files.newOutputStream(destino)) { archivo.getInputStream().transferTo(salida); }
        catch (IOException e) { throw new IllegalStateException("No se pudo guardar la evidencia interna", e); }
        String mime = archivo.getContentType() == null ? "application/octet-stream" : archivo.getContentType();
        return evidenciaRepository.save(EscalamientoEvidencia.builder().escalamiento(item).usuario(usuario).nombreArchivo(nombre).tipoMime(mime).tamano(archivo.getSize()).rutaArchivo(destino.toString()).build());
    }
    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > MAX_BYTES || !EXTENSIONES.contains(extension(Objects.requireNonNullElse(archivo.getOriginalFilename(), ""))))
            throw new BusinessException("La evidencia debe ser PNG, JPG o PDF y pesar hasta 10 MB");
    }
    private String extension(String nombre) { int punto = nombre.lastIndexOf('.'); return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT); }
}
