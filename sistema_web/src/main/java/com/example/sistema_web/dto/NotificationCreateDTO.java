// NotificationCreateDTO.java
package com.example.sistema_web.dto;

import lombok.Data;

@Data
public class NotificationCreateDTO {
    private String message;
    private String area;
    private Long asignacionId;
    private Long destinatarioId; // ID del empleado que recibirá la notificación
    private Long emisorId;       // ID del empleado que completó la tarea
}