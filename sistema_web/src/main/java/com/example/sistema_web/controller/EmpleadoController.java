package com.example.sistema_web.controller;

import com.example.sistema_web.dto.EmpleadoDTO;
import com.example.sistema_web.service.EmpleadoService;
import com.example.sistema_web.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/empleados")
@CrossOrigin(origins = "http://localhost:4200")
public class EmpleadoController {

    @Autowired
    private EmpleadoService empleadoService;

    @Autowired
    private AuditoriaService auditoriaService;

    @GetMapping
    public ResponseEntity<List<EmpleadoDTO>> getAllEmpleados() {
        try {
            List<EmpleadoDTO> empleados = empleadoService.getAllEmpleados();
            return ResponseEntity.ok(empleados);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpleadoDTO> getEmpleadoById(@PathVariable Long id) {
        try {
            EmpleadoDTO empleado = empleadoService.getEmpleadoById(id);
            return ResponseEntity.ok(empleado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<EmpleadoDTO> createEmpleado(@Valid @RequestBody EmpleadoDTO empleadoDTO, HttpServletRequest request) {
        try {
            EmpleadoDTO created = empleadoService.createEmpleado(empleadoDTO);

            String detalles = String.format("{\"nombre\":\"%s\", \"apellido\":\"%s\", \"dni\":\"%s\"}",
                    empleadoDTO.getNombre(), empleadoDTO.getApellido(), empleadoDTO.getDni());

            auditoriaService.registrar(
                    empleadoDTO.getUsuarioId().intValue(),
                    empleadoDTO.getNombre() + " " + empleadoDTO.getApellido(),
                    "CREAR_EMPLEADO",
                    "CREATE",
                    "Empleado",
                    created.getId().intValue(),
                    detalles,
                    request
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ SOLO UN MÉTODO updateEmpleado
    @PutMapping("/{id}")
    public ResponseEntity<EmpleadoDTO> updateEmpleado(@PathVariable Long id, @Valid @RequestBody EmpleadoDTO empleadoDTO, HttpServletRequest request) {
        try {
            EmpleadoDTO updated = empleadoService.updateEmpleado(id, empleadoDTO);

            // 👇 Obtén el estado anterior si lo necesitas (opcional)
            String estadoAnterior = "Activo"; // ⚠️ Reemplaza con valor real si lo tienes en el servicio
            String detalles = String.format("{\"estadoAnterior\":\"%s\", \"estadoNuevo\":\"%s\"}",
                    estadoAnterior, empleadoDTO.getEstado());

            auditoriaService.registrar(
                    empleadoDTO.getUsuarioId().intValue(),
                    empleadoDTO.getNombre() + " " + empleadoDTO.getApellido(),
                    "ACTUALIZAR_EMPLEADO",
                    "UPDATE",
                    "Empleado",
                    id.intValue(),
                    detalles,
                    request
            );

            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ✅ SOLO UN MÉTODO deleteEmpleado
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmpleado(@PathVariable Long id, HttpServletRequest request) {
        try {
            EmpleadoDTO emp = empleadoService.getEmpleadoById(id);
            auditoriaService.registrar(
                    emp.getUsuarioId().intValue(),
                    emp.getNombre() + " " + emp.getApellido(),
                    "ELIMINAR_EMPLEADO",
                    "DELETE",
                    "Empleado",
                    id.intValue(),
                    "{}",
                    request
            );
            empleadoService.deleteEmpleado(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> updateEstado(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String estado = request.get("estado");
            if (estado == null || (!"Activo".equals(estado) && !"Inactivo".equals(estado))) {
                return ResponseEntity.badRequest().build();
            }
            empleadoService.updateEstado(id, estado);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}