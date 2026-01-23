package com.example.sistema_web.service;

import com.example.sistema_web.dto.UsuarioDTO;
import com.example.sistema_web.model.Usuario;
import com.example.sistema_web.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // 🔑 Para encriptar contraseñas

    @Override
    public List<UsuarioDTO> findAll() {
        return usuarioRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UsuarioDTO> findById(Long id) {
        return usuarioRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public UsuarioDTO save(UsuarioDTO usuarioDTO) {
        // 🚨 Validar que no exista el email
        if (usuarioRepository.findByEmail(usuarioDTO.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado: " + usuarioDTO.getEmail());
        }

        // 🔑 Si no se envía un rol, asignar uno por defecto
        String rol = (usuarioDTO.getRol() != null && !usuarioDTO.getRol().isBlank())
                ? usuarioDTO.getRol()
                : "USER";

        // 🔑 Crear usuario encriptando password
        Usuario usuario = Usuario.builder()
                .id(usuarioDTO.getId())
                .nombre(usuarioDTO.getNombre())
                .email(usuarioDTO.getEmail())
                .password(passwordEncoder.encode(usuarioDTO.getPassword()))
                .rol(rol)
                .build();

        Usuario savedUsuario = usuarioRepository.save(usuario);
        return convertToDTO(savedUsuario);
    }

    @Override
    public Optional<UsuarioDTO> update(Long id, UsuarioDTO usuarioDTO) {
        return usuarioRepository.findById(id).map(existing -> {
            existing.setNombre(usuarioDTO.getNombre());
            existing.setEmail(usuarioDTO.getEmail());

            if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
            }

            if (usuarioDTO.getRol() != null && !usuarioDTO.getRol().isBlank()) {
                existing.setRol(usuarioDTO.getRol());
            }

            Usuario updated = usuarioRepository.save(existing);
            return convertToDTO(updated);
        });
    }

    @Override
    public void deleteById(Long id) {
        usuarioRepository.deleteById(id);
    }

    private UsuarioDTO convertToDTO(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                null, // 🔒 No devolvemos password
                usuario.getRol()
        );
    }
}
