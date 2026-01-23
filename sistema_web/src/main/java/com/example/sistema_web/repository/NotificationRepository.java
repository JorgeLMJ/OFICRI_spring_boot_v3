// src/main/java/com/example/sistema_web/repository/NotificationRepository.java
package com.example.sistema_web.repository;

import com.example.sistema_web.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ✅ NUEVO: Carga todas las notificaciones con empleado y emisor
    @Query("SELECT n FROM Notification n JOIN FETCH n.empleado JOIN FETCH n.emisor")
    List<Notification> findAllWithRelations();

    // ✅ NUEVO: Carga notificaciones no leídas con relaciones
    @Query("SELECT n FROM Notification n JOIN FETCH n.empleado JOIN FETCH n.emisor WHERE n.read = false")
    List<Notification> findUnreadWithRelations();

    // ✅ NUEVO: Conteo de no leídas
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.read = false")
    long countUnread();

    // ... tus otros métodos existentes ...

    @Query("SELECT n FROM Notification n JOIN FETCH n.empleado JOIN FETCH n.emisor WHERE n.empleado.id = :empleadoId AND n.read = false")
    List<Notification> findUnreadByEmpleadoId(@Param("empleadoId") Long empleadoId);

    @Query("SELECT n FROM Notification n JOIN FETCH n.empleado JOIN FETCH n.emisor WHERE n.empleado.id = :empleadoId")
    List<Notification> findByEmpleadoId(@Param("empleadoId") Long empleadoId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.empleado.id = :empleadoId AND n.read = false")
    long countUnreadByEmpleadoId(@Param("empleadoId") Long empleadoId);

    @Query("SELECT n FROM Notification n JOIN FETCH n.empleado JOIN FETCH n.emisor WHERE n.area = :area")
    List<Notification> findByArea(@Param("area") String area);

    @Query("SELECT n FROM Notification n JOIN FETCH n.empleado JOIN FETCH n.emisor WHERE n.area = :area AND n.read = false")
    List<Notification> findByAreaAndReadFalse(@Param("area") String area);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.area = :area AND n.read = false")
    long countByAreaAndReadFalse(@Param("area") String area);
    // ✅ NUEVO: Contar notificaciones que contienen "completado"
    long countByMessageContaining(String keyword);

    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT n.emisor.id, n.emisor.nombre, n.emisor.apellido, COUNT(n) " +
            "FROM Notification n " +
            "WHERE n.emisor IS NOT NULL " +
            "GROUP BY n.emisor.id, n.emisor.nombre, n.emisor.apellido " +
            "ORDER BY COUNT(n) DESC")
    List<Object[]> countByEmisorGrouped();
}