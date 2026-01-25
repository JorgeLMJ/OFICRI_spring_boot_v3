// src/main/java/com/example/sistema_web/config/JwtAuthFilter.java
package com.example.sistema_web.config;
import com.example.sistema_web.model.Empleado;
import com.example.sistema_web.model.Documento;
import com.example.sistema_web.service.DocumentoService;
import com.example.sistema_web.service.EmpleadoService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    @Autowired
    private EmpleadoService empleadoService;
    private DocumentoService documentoService;

    // ✅ Email del superusuario (excepción)
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final ThreadLocal<Long> CURRENT_EMPLEADO_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_DOCUMENTO_ID = new ThreadLocal<>();

    public static Long getCurrentEmpleadoId() {
        return CURRENT_EMPLEADO_ID.get();
    }
    public static Long getCurrentDocumentoId() {
        return CURRENT_DOCUMENTO_ID.get();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // ✅ Rutas públicas
        if (path.startsWith("/api/auth/") ||
                path.startsWith("/api/graficos/") ||
                (path.startsWith("/api/documentos/") && path.contains("/download")) ||
                (path.startsWith("/api/oficio-dosaje/") && path.contains("/callback")) ||
                (path.startsWith("/api/oficio-dosaje/") && path.contains("/download")))
        {

            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(secretKey.getBytes())
                        .parseClaimsJws(token)
                        .getBody();

                String email = claims.getSubject();
                Long empleadoId = claims.get("empleadoId", Long.class);

                // ✅ EXCEPCIÓN: Admin siempre tiene acceso
                if (ADMIN_EMAIL.equals(email)) {
                    CURRENT_EMPLEADO_ID.set(null);
                    filterChain.doFilter(request, response);
                    CURRENT_EMPLEADO_ID.remove();
                    return;
                }

                // ✅ Para otros usuarios: validar que el empleado exista y esté activo
                if (empleadoId == null) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("Acceso denegado: empleado no identificado");
                    return;
                }

                Empleado empleado = empleadoService.getEmpleadoEntityById(empleadoId);
                if (empleado == null || !"Activo".equals(empleado.getEstado())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("Tu cuenta está inactiva. Contacta al administrador.");
                    return;
                }

                CURRENT_EMPLEADO_ID.set(empleadoId);
                filterChain.doFilter(request, response);
                CURRENT_EMPLEADO_ID.remove();

            } catch (Exception e) {
                CURRENT_EMPLEADO_ID.remove();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token inválido o expirado");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token no proporcionado");
        }
    }
}