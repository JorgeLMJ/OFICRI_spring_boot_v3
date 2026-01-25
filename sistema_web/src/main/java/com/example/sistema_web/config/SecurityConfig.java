package com.example.sistema_web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; // 👈 Importante
import org.springframework.web.cors.CorsConfigurationSource; // 👈 Importante
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // 👈 Importante

import java.util.Arrays; // 👈 Importante
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/api/documentos/*/download")
                .requestMatchers("/api/documentos/*/save-callback")
                .requestMatchers("/api/onlyoffice/**")
                .requestMatchers("/api/oficio-dosaje/*/download")
                .requestMatchers("/api/oficio-dosaje/*/save-callback");
    }

    // 👇👇👇 1. DEFINIR LA FUENTE DE CONFIGURACIÓN CORS AQUÍ 👇👇👇
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Orígenes permitidos: Angular, Docker/OnlyOffice, y tu IP local
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",
                "http://localhost:8081"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        configuration.setExposedHeaders(Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    // 👆👆👆 FIN DE CORS 👆👆👆

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // 👇👇👇 2. VINCULAR LA CONFIGURACIÓN AQUÍ 👇👇👇
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/empleados/**").permitAll()
                        .requestMatchers("/api/documentos/**").permitAll()
                        .requestMatchers("/api/onlyoffice/**").permitAll()
                        .requestMatchers("/api/oficio-dosaje/**").permitAll()
                        .requestMatchers("/api/asignaciones-dosaje/**").permitAll()
                        .requestMatchers("/api/asignaciones-toxicologia/**").permitAll()
                        .requestMatchers("/api/usuarios/**").permitAll()
                        .requestMatchers("/api/roles/**").permitAll()
                        .requestMatchers("/api/notifications/**").permitAll()
                        .requestMatchers("/api/graficos/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/documentos/*/save-callback").permitAll()
                        .requestMatchers("/api/documentos/*/download").permitAll()
                        .requestMatchers("/api/oficio-dosaje/*/download").permitAll()
                        .requestMatchers("/api/oficio-dosaje/*/callback").permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}