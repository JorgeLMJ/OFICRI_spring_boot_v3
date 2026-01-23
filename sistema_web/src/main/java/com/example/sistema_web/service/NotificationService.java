// src/main/java/com/example/sistema_web/service/NotificationService.java
package com.example.sistema_web.service;

import com.example.sistema_web.model.Empleado;
import com.example.sistema_web.model.Notification;
import com.example.sistema_web.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepo;

    public Notification crearNotificacion(String message, String area, Long asignacionId, Empleado destinatario, Empleado emisor) {
        Notification notificacion = new Notification(message, area, asignacionId, destinatario, emisor);
        return notificationRepo.save(notificacion);
    }

    // ✅ Devuelve TODAS las notificaciones con relaciones cargadas
    public List<Notification> getAllNotifications() {
        return notificationRepo.findAllWithRelations();
    }

    // ✅ Devuelve notificaciones no leídas con relaciones
    public List<Notification> getUnreadNotifications() {
        return notificationRepo.findUnreadWithRelations();
    }

    // ✅ Conteo de no leídas
    public long countUnreadNotifications() {
        return notificationRepo.countUnread();
    }

    public List<Notification> obtenerNotificacionesNoLeidasPorEmpleado(Long empleadoId) {
        return notificationRepo.findUnreadByEmpleadoId(empleadoId);
    }

    public List<Notification> obtenerNotificacionesPorEmpleado(Long empleadoId) {
        return notificationRepo.findByEmpleadoId(empleadoId);
    }

    public List<Notification> getNotificationsByArea(String area) {
        return notificationRepo.findByArea(area);
    }

    public List<Notification> getUnreadNotificationsByArea(String area) {
        return notificationRepo.findByAreaAndReadFalse(area);
    }

    public Notification markAsRead(Long id) {
        Notification notification = notificationRepo.findById(id).orElse(null);
        if (notification != null) {
            notification.setRead(true);
            return notificationRepo.save(notification);
        }
        throw new RuntimeException("Notificación no encontrada con ID: " + id);
    }

    public long countUnreadByArea(String area) {
        return notificationRepo.countByAreaAndReadFalse(area);
    }

    public long countUnreadByEmpleado(Long empleadoId) {
        return notificationRepo.countUnreadByEmpleadoId(empleadoId);
    }
}