package com.example.sistema_web.service;

import com.example.sistema_web.model.Usuario;
import com.example.sistema_web.dto.UsuarioDTO;
import java.util.List;
import java.util.Optional;

public interface UsuarioService {
    List<UsuarioDTO> findAll();
    Optional<UsuarioDTO> findById(Long id);
    UsuarioDTO save(UsuarioDTO usuarioDTO);
    Optional<UsuarioDTO> update(Long id, UsuarioDTO usuarioDTO);
    void deleteById(Long id);
}