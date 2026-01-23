package com.example.sistema_web.service;

import com.example.sistema_web.model.Auditoria;
import com.example.sistema_web.repository.AuditoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepo;

    // 👇 Método para registrar (ya lo tenías)
    public void registrar(
            Integer usuarioId,
            String nombreUsuario,
            String accion,
            String tipoAccion,
            String entidad,
            Integer entidadId,
            String detalles,
            HttpServletRequest request
    ) {
        Auditoria a = new Auditoria();
        a.setUsuarioId(usuarioId);
        a.setNombreUsuario(nombreUsuario);
        a.setAccion(accion);
        a.setTipoAccion(tipoAccion);
        a.setEntidad(entidad);
        a.setEntidadId(entidadId);
        a.setDetalles(detalles);
        a.setSessionId(request.getRequestedSessionId());
        a.setIp(request.getRemoteAddr());
        a.setUserAgent(request.getHeader("User-Agent"));
        a.setFecha(LocalDateTime.now());
        auditoriaRepo.save(a);
    }

    // 👇 Nuevos métodos para el controlador
    public List<Auditoria> getAllAuditorias() {
        return auditoriaRepo.findAllByOrderByFechaDesc();
    }

    public List<Auditoria> getAuditoriasByTipo(String tipoAccion) {
        return auditoriaRepo.findByTipoAccionOrderByFechaDesc(tipoAccion);
    }

    public List<Auditoria> getAuditoriasPorRango(String fechaInicioStr, String fechaFinStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime inicio = LocalDateTime.parse(fechaInicioStr + "T00:00:00");
        LocalDateTime fin = LocalDateTime.parse(fechaFinStr + "T23:59:59");
        return auditoriaRepo.findByFechaBetweenOrderByFechaDesc(inicio, fin);
    }
}