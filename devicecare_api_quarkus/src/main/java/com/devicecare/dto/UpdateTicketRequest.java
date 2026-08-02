package com.devicecare.dto;

import com.devicecare.entity.enums.TicketPriority;

public record UpdateTicketRequest(
        String title,
        String description,
        TicketPriority priority,
        String assignedTo
) {
}
