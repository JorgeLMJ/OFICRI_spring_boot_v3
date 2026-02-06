package com.example.sistema_web.controller;

import com.example.sistema_web.dto.LoginRequest;
import com.example.sistema_web.dto.LoginResponse;
import com.example.sistema_web.dto.UsuarioDTO;
import com.example.sistema_web.model.Usuario;
import com.example.sistema_web.repository.UsuarioRepository;
import com.example.sistema_web.service.AuthService;
import com.example.sistema_web.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.authenticate(loginRequest);
        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).build();
        }
    }

    // ============================
    // REGISTRO
    // ============================
    @PostMapping("/registro")
    public ResponseEntity<String> registrar(@RequestBody UsuarioDTO usuarioDTO) {
        try {
            authService.registrarUsuario(usuarioDTO);
            return ResponseEntity.ok("Registro exitoso ✅");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============================
    // OLVIDÉ MI CONTRASEÑA
    // ============================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Usuario no encontrado"));
        }
        Usuario usuario = usuarioOpt.get();
        String token = UUID.randomUUID().toString();
        usuario.setResetToken(token);
        usuario.setTokenExpiration(LocalDateTime.now().plusMinutes(30));
        usuarioRepository.save(usuario);

        String link = "http://localhost:4200/reset-password?token=" + token;
        emailService.enviarCorreo(
                usuario.getEmail(),
                "Recuperación de contraseña",
                "Hola " + usuario.getNombre() + ",\n\n" +
                        "Haz clic en el siguiente enlace para restablecer tu contraseña:\n" +
                        link +
                        "\n\nEste enlace expirará en 30 minutos."
        );
        return ResponseEntity.ok(Map.of("message", "Se ha enviado un enlace de recuperación a tu correo 📧"));
    }

    // ============================
    // RESTABLECER CONTRASEÑA
    // ============================
    // ✅ Responder con JSON en lugar de texto plano
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String nuevaPassword = request.get("newPassword");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByResetToken(token);

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("message", "Token inválido o no encontrado"));
        }

        Usuario usuario = usuarioOpt.get();

        if (usuario.getTokenExpiration() == null || usuario.getTokenExpiration().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body(Map.of("message", "Token expirado"));
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setResetToken(null);
        usuario.setTokenExpiration(null);
        usuarioRepository.save(usuario);

        // 👇 devolvemos JSON
        return ResponseEntity.ok(Map.of("message", "Contraseña restablecida correctamente ✅"));
    }

    // ============================
    // TEST ENVÍO DE CORREOS
    // ============================
    @GetMapping("/test-email")
    public ResponseEntity<String> testEmail() {
        try {
            emailService.enviarCorreo(
                    "luisjaraidme@gmail.com",
                    "Prueba Spring Boot",
                    "✅ El envío funciona correctamente."
            );
            return ResponseEntity.ok("Correo enviado correctamente.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

}
