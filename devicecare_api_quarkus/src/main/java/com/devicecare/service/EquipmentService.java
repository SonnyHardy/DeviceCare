package com.devicecare.service;

import com.devicecare.dto.EquipmentDTO;
import com.devicecare.dto.EquipmentRequest;
import com.devicecare.entity.Equipment;
import com.devicecare.entity.enums.EquipmentStatus;
import com.devicecare.exception.EquipmentNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EquipmentService {

    // List all equipments based on status and type
    public List<EquipmentDTO> getAllEquipments(EquipmentStatus status, String type) {
        List<Equipment> results;
        if (status != null && type != null) {
            results = Equipment.list("status = ?1 and type = ?2", status, type);
        } else if (status != null) {
            results = Equipment.list("status", status);
        } else if (type != null) {
            results = Equipment.list("type", type);
        } else {
            results = Equipment.listAll();
        }
        return results.stream().map(EquipmentDTO::from).toList();
    }

    // Get equipment by ID
    public EquipmentDTO getEquipmentById(UUID id) {
        Equipment equipment = Equipment.findById(id);
        if (equipment == null) {
            throw new EquipmentNotFoundException("Equipment " + id + " not found");
        }
        return EquipmentDTO.from(equipment);
    }

    // Create a new equipment
    @Transactional
    public EquipmentDTO createEquipment(EquipmentRequest request) {
        Equipment equipment = new Equipment();
        equipment.name = request.name();
        equipment.type = request.type();
        equipment.serialNumber = request.serialNumber();
        equipment.location = request.location();
        equipment.purchaseDate = request.purchaseDate();
        equipment.persist();

        return EquipmentDTO.from(equipment);
    }

    // Update an existing equipment
    @Transactional
    public EquipmentDTO updateEquipment(UUID id, EquipmentRequest request) {
        if (id == null) throw new IllegalArgumentException("Equipment ID cannot be null");
        Equipment equipment = Equipment.findById(id);
        if (equipment == null) {
            throw new EquipmentNotFoundException("Equipment " + id + " not found");
        }
        equipment.name = request.name();
        equipment.type = request.type();
        equipment.serialNumber = request.serialNumber();
        equipment.location = request.location();
        equipment.purchaseDate = request.purchaseDate();

        return EquipmentDTO.from(equipment);
    }

    // Delete an equipment
    @Transactional
    public boolean deleteEquipment(UUID id) {
        if (id == null) throw new IllegalArgumentException("Equipment ID cannot be null");
        return Equipment.deleteById(id);
    }

}
