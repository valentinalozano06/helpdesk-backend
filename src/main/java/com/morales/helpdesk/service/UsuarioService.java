package com.morales.helpdesk.service;

import com.morales.helpdesk.dto.auth.RegisterRequest;
import com.morales.helpdesk.dto.usuario.GuardarUsuarioRequest;
import com.morales.helpdesk.model.Usuario;

import java.util.List;

public interface UsuarioService {
    List<Usuario> listarTecnicos();
    List<Usuario> listarTodos();
    Usuario obtenerPorId(Long id);
    Usuario crear(RegisterRequest request);
    Usuario actualizar(Long id, GuardarUsuarioRequest request);
    Usuario cambiarEstado(Long id, String nuevoEstado);
    boolean eliminar(Long id);
}
