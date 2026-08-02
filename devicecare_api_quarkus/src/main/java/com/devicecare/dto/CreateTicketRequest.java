package com.devicecare.dto;

import com.devicecare.entity.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTicketRequest(
        @NotNull
        UUID equipmentId,
        @NotBlank
        String title,
        String description,
        TicketPriority priority,
        String assignedTo
) {
}
