// src/main/java/com/example/sistema_web/controller/NotificationController.java
package com.example.sistema_web.controller;

import com.example.sistema_web.dto.NotificationCreateDTO;
import com.example.sistema_web.model.Empleado;
import com.example.sistema_web.model.Notification;
import com.example.sistema_web.service.EmpleadoService;
import com.example.sistema_web.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    private EmpleadoService empleadoService;

    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody NotificationCreateDTO dto) {
        try {
            Empleado destinatario = empleadoService.getEmpleadoEntityById(dto.getDestinatarioId());
            Empleado emisor = empleadoService.getEmpleadoEntityById(dto.getEmisorId());

            Notification notificacion = notificationService.crearNotificacion(
                    dto.getMessage(),
                    dto.getArea(),
                    dto.getAsignacionId(),
                    destinatario,
                    emisor
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(notificacion);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ NUEVO: Devuelve TODAS las notificaciones
    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        List<Notification> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    // ✅ NUEVO: Devuelve notificaciones no leídas
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications() {
        List<Notification> notifications = notificationService.getUnreadNotifications();
        return ResponseEntity.ok(notifications);
    }

    // ✅ NUEVO: Conteo de no leídas
    @GetMapping("/unread/count")
    public ResponseEntity<Long> countUnreadNotifications() {
        long count = notificationService.countUnreadNotifications();
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/area/{area}")
    public ResponseEntity<List<Notification>> getNotificationsByArea(@PathVariable String area) {
        List<Notification> notifications = notificationService.getNotificationsByArea(area);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/area/{area}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotificationsByArea(@PathVariable String area) {
        List<Notification> notifications = notificationService.getUnreadNotificationsByArea(area);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/area/{area}/count-unread")
    public ResponseEntity<Long> countUnreadByArea(@PathVariable String area) {
        long count = notificationService.countUnreadByArea(area);
        return ResponseEntity.ok(count);
    }
}