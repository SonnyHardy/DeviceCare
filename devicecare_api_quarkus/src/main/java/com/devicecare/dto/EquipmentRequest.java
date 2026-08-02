package com.devicecare.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record EquipmentRequest(
        @NotBlank
        String name,
        @NotBlank
        String type,
        @NotBlank
        String serialNumber,
        String location,
        LocalDate purchaseDate
) {
}
