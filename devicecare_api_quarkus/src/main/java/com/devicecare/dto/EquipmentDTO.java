package com.devicecare.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.devicecare.entity.Equipment;
import com.devicecare.entity.enums.EquipmentStatus;

public class EquipmentDTO {

    public UUID id;
    public String name;
    public String type;
    public String serialNumber;
    public EquipmentStatus status;
    public String location;
    public LocalDate purchaseDate;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public static EquipmentDTO from(Equipment e) {
        EquipmentDTO dto = new EquipmentDTO();
        dto.id = e.id;
        dto.name = e.name;
        dto.type = e.type;
        dto.serialNumber = e.serialNumber;
        dto.status = e.status;
        dto.location = e.location;
        dto.purchaseDate = e.purchaseDate;
        dto.createdAt = e.createdAt;
        dto.updatedAt = e.updatedAt;
        return dto;
    }
}