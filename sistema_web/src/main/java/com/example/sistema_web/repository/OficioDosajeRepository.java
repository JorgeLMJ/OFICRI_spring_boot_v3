// src/main/java/com/example/sistema_web/repository/OficioDosajeRepository.java
package com.example.sistema_web.repository;

import com.example.sistema_web.model.OficioDosaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OficioDosajeRepository extends JpaRepository<OficioDosaje, Long> {
}