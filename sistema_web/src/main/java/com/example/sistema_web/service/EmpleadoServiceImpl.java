// src/main/java/com/example/sistema_web/service/EmpleadoServiceImpl.java
package com.example.sistema_web.service;

import com.example.sistema_web.dto.EmpleadoDTO;
import com.example.sistema_web.model.Empleado;
import com.example.sistema_web.model.Usuario;
import com.example.sistema_web.repository.EmpleadoRepository;
import com.example.sistema_web.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmpleadoServiceImpl implements EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public List<EmpleadoDTO> getAllEmpleados() {
        return empleadoRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EmpleadoDTO getEmpleadoById(Long id) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + id));
        return convertToDTO(empleado); // ✅ Siempre devuelve DTO
    }

    @Override
    public EmpleadoDTO createEmpleado(EmpleadoDTO empleadoDTO) {
        if (empleadoDTO.getUsuarioId() == null) {
            throw new RuntimeException("usuarioId es obligatorio");
        }

        Usuario usuario = usuarioRepository.findById(empleadoDTO.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + empleadoDTO.getUsuarioId()));

        if (empleadoRepository.findByDni(empleadoDTO.getDni()) != null) {
            throw new RuntimeException("Ya existe un empleado con el DNI: " + empleadoDTO.getDni());
        }

        Empleado empleado = new Empleado();
        empleado.setNombre(empleadoDTO.getNombre());
        empleado.setApellido(empleadoDTO.getApellido());
        empleado.setDni(empleadoDTO.getDni());
        empleado.setCargo(empleadoDTO.getCargo());
        empleado.setTelefono(empleadoDTO.getTelefono());
        empleado.setEstado(empleadoDTO.getEstado());
        empleado.setUsuario(usuario);

        Empleado savedEmpleado = empleadoRepository.save(empleado);
        return convertToDTO(savedEmpleado);
    }

    @Override
    public EmpleadoDTO updateEmpleado(Long id, EmpleadoDTO empleadoDTO) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + id));

        Empleado existingByDni = empleadoRepository.findByDni(empleadoDTO.getDni());
        if (existingByDni != null && !existingByDni.getId().equals(id)) {
            throw new RuntimeException("Ya existe otro empleado con el DNI: " + empleadoDTO.getDni());
        }

        empleado.setNombre(empleadoDTO.getNombre());
        empleado.setApellido(empleadoDTO.getApellido());
        empleado.setDni(empleadoDTO.getDni());
        empleado.setCargo(empleadoDTO.getCargo());
        empleado.setTelefono(empleadoDTO.getTelefono());
        empleado.setEstado(empleadoDTO.getEstado());

        if (empleadoDTO.getUsuarioId() != null) {
            Usuario usuario = usuarioRepository.findById(empleadoDTO.getUsuarioId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + empleadoDTO.getUsuarioId()));
            empleado.setUsuario(usuario);
        }

        Empleado updatedEmpleado = empleadoRepository.save(empleado);
        return convertToDTO(updatedEmpleado);
    }

    @Override
    public void deleteEmpleado(Long id) {
        if (!empleadoRepository.existsById(id)) {
            throw new RuntimeException("Empleado no encontrado con ID: " + id);
        }
        empleadoRepository.deleteById(id);
    }

    @Override
    public Empleado findByCargo(String cargo) {
        return empleadoRepository.findByCargo(cargo);
    }

    @Override
    public void updateEstado(Long id, String estado) {
        if (!"Activo".equals(estado) && !"Inactivo".equals(estado)) {
            throw new RuntimeException("Estado inválido. Use 'Activo' o 'Inactivo'.");
        }

        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + id));
        empleado.setEstado(estado);
        empleadoRepository.save(empleado);
    }

    @Override
    public Empleado getEmpleadoEntityById(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
    }

    // ✅ ELIMINADO: el método duplicado que devolvía Empleado

    private EmpleadoDTO convertToDTO(Empleado empleado) {
        EmpleadoDTO dto = new EmpleadoDTO();
        dto.setId(empleado.getId());
        dto.setNombre(empleado.getNombre());
        dto.setApellido(empleado.getApellido());
        dto.setDni(empleado.getDni());
        dto.setCargo(empleado.getCargo());
        dto.setTelefono(empleado.getTelefono());
        dto.setEstado(empleado.getEstado());
        if (empleado.getUsuario() != null) {
            dto.setUsuarioId(empleado.getUsuario().getId());
            dto.setUsuarioEmail(empleado.getUsuario().getEmail());
        }
        return dto;
    }
}  