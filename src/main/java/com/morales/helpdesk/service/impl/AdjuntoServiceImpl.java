package com.morales.helpdesk.service.impl;

import com.morales.helpdesk.dto.ticket.AdjuntoResponse;
import com.morales.helpdesk.dto.ticket.ContenidoAdjunto;
import com.morales.helpdesk.exception.BusinessException;
import com.morales.helpdesk.exception.ResourceNotFoundException;
import com.morales.helpdesk.mapper.TicketMapper;
import com.morales.helpdesk.model.Adjunto;
import com.morales.helpdesk.model.HistorialCambio;
import com.morales.helpdesk.model.Ticket;
import com.morales.helpdesk.model.Usuario;
import com.morales.helpdesk.repository.AdjuntoRepository;
import com.morales.helpdesk.repository.HistorialCambioRepository;
import com.morales.helpdesk.repository.TicketRepository;
import com.morales.helpdesk.repository.UsuarioRepository;
import com.morales.helpdesk.security.CustomUserDetails;
import com.morales.helpdesk.service.AdjuntoService;
import com.morales.helpdesk.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Reglas de negocio de la evidencia adjunta por ticket. Cualquier rol con
 * acceso de lectura al ticket puede adjuntar y ver evidencia (el
 * solicitante agrega material y el tecnico de TI lo revisa); el archivo
 * se guarda en el directorio de uploads y los metadatos en la tabla
 * adjuntos. Cada carga queda registrada en el historial y notifica a la
 * contraparte del ticket.
 */
@Service
@RequiredArgsConstructor
public class AdjuntoServiceImpl implements AdjuntoService {

    private static final Logger log = LoggerFactory.getLogger(AdjuntoServiceImpl.class);

    static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("png", "jpg", "jpeg");
    static final long TAMANO_MAXIMO_BYTES = 10L * 1024 * 1024; // 10 MB por archivo
    static final int MAXIMO_POR_CARGA = 5;
    static final int MAXIMO_POR_TICKET = 10;

    private final AdjuntoRepository adjuntoRepository;
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialCambioRepository historialCambioRepository;
    private final NotificacionService notificacionService;
    private final TicketMapper ticketMapper;

    @Value("${app.uploads.dir:uploads}")
    private String directorioAlmacen;

    @Override
    @Transactional
    public List<AdjuntoResponse> subir(Long ticketId, List<MultipartFile> archivos, Authentication auth) {
        Ticket ticket = buscarTicket(ticketId);
        Usuario usuario = usuarioActual(auth);
        validarEdicion(ticket, usuario);

        if (archivos == null || archivos.isEmpty()) {
            throw new BusinessException("Debe adjuntar al menos un archivo de evidencia");
        }
        if (archivos.size() > MAXIMO_POR_CARGA) {
            throw new BusinessException("Solo se pueden adjuntar hasta " + MAXIMO_POR_CARGA + " archivos por carga");
        }
        if (adjuntoRepository.countByTicketId(ticketId) + archivos.size() > MAXIMO_POR_TICKET) {
            throw new BusinessException("El ticket admite hasta " + MAXIMO_POR_TICKET + " archivos de evidencia en total");
        }
        archivos.forEach(this::validar);

        Path carpeta = carpetaDelTicket(ticketId);
        List<Adjunto> guardados = archivos.stream().map(archivo -> guardar(ticket, usuario, archivo, carpeta)).toList();
        guardados.forEach(adjunto -> registrarEvidencia(ticket, usuario, adjunto.getNombreArchivo()));

        log.info("{} archivo(s) de evidencia adjuntados al ticket {} por {}",
                guardados.size(), ticket.getCodigoUnico(), usuario.getEmail());

        Usuario aNotificar = usuario.getId().equals(ticket.getSolicitante().getId())
                ? ticket.getAsignadoA() : ticket.getSolicitante();
        if (aNotificar != null) {
            notificacionService.notificarEvento(aNotificar, "ticket_evidencia",
                    "Evidencia adjuntada al ticket " + ticket.getCodigoUnico() + ": " + ticket.getAsunto(), ticket.getId());
        }

        return guardados.stream().map(ticketMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdjuntoResponse> listar(Long ticketId, Authentication auth) {
        Ticket ticket = buscarTicket(ticketId);
        validarLectura(ticket, usuarioActual(auth));
        return adjuntoRepository.findByTicketIdOrderByFechaCreacionAscIdAsc(ticketId).stream()
                .map(ticketMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContenidoAdjunto descargar(Long id, Authentication auth) {
        Adjunto adjunto = adjuntoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo de evidencia no encontrado: " + id));
        validarLectura(adjunto.getTicket(), usuarioActual(auth));

        Path ruta = Paths.get(adjunto.getRutaArchivo());
        if (!Files.isReadable(ruta)) {
            throw new ResourceNotFoundException("El archivo de evidencia ya no esta disponible en el servidor");
        }
        return new ContenidoAdjunto(ticketMapper.toResponse(adjunto), new FileSystemResource(ruta));
    }

    @Override
    @Transactional
    public void eliminar(Long id, Authentication auth) {
        Adjunto adjunto = adjuntoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidencia no encontrada"));
        Usuario usuario = usuarioActual(auth);
        validarEdicion(adjunto.getTicket(), usuario);
        if (!adjunto.getUsuario().getId().equals(usuario.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Solo puedes eliminar tu evidencia");
        }
        historialCambioRepository.save(HistorialCambio.builder().ticket(adjunto.getTicket()).usuario(usuario)
                .campoModificado("evidencia").valorAnterior(adjunto.getNombreArchivo()).valorNuevo("Eliminada").build());
        adjuntoRepository.delete(adjunto);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit() {
                    try { Files.deleteIfExists(Paths.get(adjunto.getRutaArchivo())); }
                    catch (IOException e) { log.error("No se pudo eliminar archivo de evidencia {}", id, e); }
                }
            });
    }

    private void validarEdicion(Ticket ticket, Usuario usuario) {
        if (!"rol_solicitante".equals(usuario.getRol().getNombre())
                || !ticket.getSolicitante().getId().equals(usuario.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Solo el solicitante dueño puede modificar la evidencia");
        }
    }

    // -------------------------------------------------- almacenamiento

    private Adjunto guardar(Ticket ticket, Usuario usuario, MultipartFile archivo, Path carpeta) {
        String nombreOriginal = limpiarNombre(archivo.getOriginalFilename());
        String nombreAlmacen = UUID.randomUUID() + "." + extensionDe(nombreOriginal);
        Path destino = carpeta.resolve(nombreAlmacen);
        try (OutputStream salida = Files.newOutputStream(destino)) {
            archivo.getInputStream().transferTo(salida);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo " + nombreOriginal, e);
        }
        Adjunto adjunto = Adjunto.builder()
                .ticket(ticket)
                .usuario(usuario)
                .nombreArchivo(nombreOriginal)
                .tipoMime(StringUtils.hasText(archivo.getContentType()) ? archivo.getContentType() : "application/octet-stream")
                .tamano(archivo.getSize())
                .rutaArchivo(destino.toString())
                .build();
        return adjuntoRepository.save(adjunto);
    }

    private Path carpetaDelTicket(Long ticketId) {
        Path carpeta = Paths.get(directorioAlmacen).resolve(String.valueOf(ticketId));
        try {
            return Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo preparar el almacen de evidencia", e);
        }
    }

    // -------------------------------------------------- validaciones

    private void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("Hay un archivo vacio en la carga");
        }
        String nombre = limpiarNombre(archivo.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extensionDe(nombre))) {
            throw new BusinessException("Formato no permitido (" + nombre + "): solo fotos PNG, JPG o JPEG");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new BusinessException("El archivo " + nombre + " supera el maximo de 10 MB");
        }
    }

    private String limpiarNombre(String nombre) {
        String limpio = (nombre == null || nombre.isBlank()) ? "evidencia" : StringUtils.cleanPath(nombre.trim());
        int barra = limpio.lastIndexOf('/');
        if (barra >= 0) limpio = limpio.substring(barra + 1);
        if (limpio.length() > 180) {
            String ext = extensionDe(limpio);
            limpio = limpio.substring(0, 170) + (ext.isEmpty() ? "" : "." + ext);
        }
        return limpio;
    }

    private String extensionDe(String nombre) {
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) return "";
        return nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    private void validarLectura(Ticket t, Usuario u) {
        String rol = u.getRol().getNombre();
        if (Set.of("rol_admin", "rol_jefatura").contains(rol) || t.getSolicitante().getId().equals(u.getId())) return;
        if ("rol_tecnico".equals(rol) && t.getAsignadoA() != null && t.getAsignadoA().getId().equals(u.getId())) return;
        throw new org.springframework.security.access.AccessDeniedException("Ticket de otro usuario");
    }

    private void registrarEvidencia(Ticket ticket, Usuario usuario, String nombreArchivo) {
        historialCambioRepository.save(HistorialCambio.builder()
                .ticket(ticket).usuario(usuario)
                .campoModificado("evidencia").valorAnterior(null).valorNuevo(nombreArchivo)
                .build());
    }

    private Ticket buscarTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado: " + id));
    }

    private Usuario usuarioActual(Authentication auth) {
        Long id = ((CustomUserDetails) auth.getPrincipal()).getId();
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }
}
