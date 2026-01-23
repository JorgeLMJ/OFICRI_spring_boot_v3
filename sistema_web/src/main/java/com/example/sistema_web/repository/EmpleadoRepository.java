package com.example.sistema_web.repository;

import com.example.sistema_web.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Empleado findByDni(String dni);
    boolean existsById(Long id);
    Empleado findByCargo(String cargo);
    Empleado findByUsuarioId(Long usuarioId);
}