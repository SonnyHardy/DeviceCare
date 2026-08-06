package com.devicecare.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.devicecare.entity.MaintenanceTicket;
import com.devicecare.entity.enums.TicketPriority;
import com.devicecare.entity.enums.TicketStatus;


public class MaintenanceTicketDTO {

    public UUID id;
    public UUID equipmentId;
    public String title;
    public String description;
    public TicketPriority priority;
    public TicketStatus status;
    public String assignedTo;
    public LocalDateTime createdAt;
    public LocalDateTime resolvedAt;

    public static MaintenanceTicketDTO from(MaintenanceTicket t) {
        MaintenanceTicketDTO dto = new MaintenanceTicketDTO();
        dto.id = t.id;
        dto.equipmentId = t.equipment.id;
        dto.title = t.title;
        dto.description = t.description;
        dto.priority = t.priority;
        dto.status = t.status;
        dto.assignedTo = t.assignedTo;
        dto.createdAt = t.createdAt;
        dto.resolvedAt = t.resolvedAt;
        return dto;
    }
}