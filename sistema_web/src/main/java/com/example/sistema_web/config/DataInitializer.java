package com.example.sistema_web.config;

import com.example.sistema_web.model.Usuario;
import com.example.sistema_web.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Crear usuario admin si no existe
        usuarioRepository.findByEmail("admin@gmail.com").ifPresentOrElse(
                user -> System.out.println("✅ Usuario admin ya existe: " + user.getEmail()),
                () -> {
                    Usuario admin = new Usuario();
                    admin.setNombre("Administrador");
                    admin.setEmail("admin@gmail.com");
                    admin.setPassword(passwordEncoder.encode("admin")); // 🔑 Encriptar contraseña
                    admin.setRol("Administrador"); // 👈 Ahora es un String

                    usuarioRepository.save(admin);
                    System.out.println("🚀 Usuario admin creado con éxito!");
                }
        );
    }
}
