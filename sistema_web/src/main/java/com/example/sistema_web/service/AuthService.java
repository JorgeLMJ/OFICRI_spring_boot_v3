// src/main/java/com/example/sistema_web/service/AuthService.java
package com.example.sistema_web.service;

import com.example.sistema_web.dto.LoginRequest;
import com.example.sistema_web.dto.LoginResponse;
import com.example.sistema_web.dto.UsuarioDTO;
import com.example.sistema_web.model.Usuario;
import com.example.sistema_web.model.Empleado;
import com.example.sistema_web.repository.UsuarioRepository;
import com.example.sistema_web.repository.EmpleadoRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secretKey;

    // ✅ Email del superusuario (excepción)
    private static final String ADMIN_EMAIL = "admin@gmail.com";

    public LoginResponse authenticate(LoginRequest loginRequest) {
        logger.info("Intentando autenticar usuario: {}", loginRequest.getUsername());

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(loginRequest.getUsername());
        if (usuarioOpt.isEmpty()) {
            logger.warn("Usuario no encontrado: {}", loginRequest.getUsername());
            return null;
        }

        Usuario usuario = usuarioOpt.get();
        if (!passwordEncoder.matches(loginRequest.getPassword(), usuario.getPassword())) {
            logger.warn("Contraseña incorrecta para usuario: {}", loginRequest.getUsername());
            return null;
        }

        // ✅ EXCEPCIÓN: Admin siempre tiene acceso
        if (ADMIN_EMAIL.equals(usuario.getEmail())) {
            String token = generateToken(usuario.getEmail(), null);
            logger.info("✅ Autenticación exitosa para ADMIN: {}", usuario.getEmail());
            return new LoginResponse(
                    token,
                    usuario.getNombre(),
                    usuario.getEmail(),
                    usuario.getRol(),
                    null
            );
        }

        // ✅ Para otros usuarios: validar empleado y estado
        Empleado empleado = empleadoRepository.findByUsuarioId(usuario.getId());
        if (empleado == null) {
            logger.warn("❌ Usuario sin empleado asociado: {}", usuario.getEmail());
            return null;
        }

        if (!"Activo".equals(empleado.getEstado())) {
            logger.warn("🔒 Acceso denegado: empleado inactivo - {}", usuario.getEmail());
            return null;
        }

        String token = generateToken(usuario.getEmail(), empleado.getId());
        logger.info("✅ Autenticación exitosa para usuario: {}", usuario.getEmail());

        return new LoginResponse(
                token,
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                empleado.getId()
        );
    }

    @Transactional
    public void registrarUsuario(UsuarioDTO usuarioDTO) {
        logger.info("Registrando nuevo usuario: {}", usuarioDTO.getEmail());

        if (usuarioRepository.findByEmail(usuarioDTO.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado: " + usuarioDTO.getEmail());
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        usuario.setRol(usuarioDTO.getRol() != null ? usuarioDTO.getRol() : "USER");

        try {
            usuarioRepository.save(usuario);
            logger.info("Usuario registrado exitosamente: {}", usuario.getEmail());
        } catch (Exception e) {
            logger.error("Error al registrar usuario: {}", e.getMessage());
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage());
        }
    }

    private String generateToken(String email, Long empleadoId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("empleadoId", empleadoId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(SignatureAlgorithm.HS512, secretKey.getBytes())
                .compact();
    }
}