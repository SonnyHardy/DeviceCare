package com.devicecare.service;

import com.devicecare.dto.CreateTicketRequest;
import com.devicecare.dto.MaintenanceTicketDTO;
import com.devicecare.dto.UpdateTicketRequest;
import com.devicecare.entity.Equipment;
import com.devicecare.entity.MaintenanceTicket;
import com.devicecare.entity.enums.EquipmentStatus;
import com.devicecare.entity.enums.TicketPriority;
import com.devicecare.entity.enums.TicketStatus;
import com.devicecare.exception.EquipmentNotFoundException;
import com.devicecare.exception.MaintenanceTicketNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MaintenanceTicketService {

    public List<MaintenanceTicketDTO> listTickets(UUID equipmentId, TicketStatus status, TicketPriority priority) {
        List<MaintenanceTicket> results = equipmentId != null
                ? MaintenanceTicket.findByEquipment(equipmentId)
                : MaintenanceTicket.listAll();

        return results.stream()
                .filter(t -> status == null || t.status == status)
                .filter(t -> priority == null || t.priority == priority)
                .map(MaintenanceTicketDTO::from)
                .toList();
    }

    public MaintenanceTicketDTO getTicketById(UUID id) {
        if (id == null) throw new IllegalArgumentException("Ticket ID cannot be null");
        MaintenanceTicket ticket = MaintenanceTicket.findById(id);
        if (ticket == null) {
            throw new MaintenanceTicketNotFoundException("Ticket " + id + " not found");
        }
        return MaintenanceTicketDTO.from(ticket);
    }

    @Transactional
    public MaintenanceTicket createTicket(CreateTicketRequest request) {
        Equipment equipment = Equipment.findById(request.equipmentId());
        if (equipment == null) throw new EquipmentNotFoundException("Equipment " + request.equipmentId() + " not found");

        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.equipment = equipment;
        ticket.title = request.title();
        ticket.description = request.description();
        if (request.priority() != null) ticket.priority = request.priority();
        ticket.assignedTo = request.assignedTo();
        ticket.persist();

        // Business rule: opening a ticket on operational equipment puts it into maintenance
        if (equipment.status == EquipmentStatus.OPERATIONAL) {
            equipment.status = EquipmentStatus.IN_MAINTENANCE;
        }

        return ticket;
    }

    @Transactional
    public MaintenanceTicket updateTicketStatus(UUID ticketId, TicketStatus newStatus) {
        if (ticketId == null) throw new IllegalArgumentException("Ticket ID cannot be null");

        MaintenanceTicket ticket = MaintenanceTicket.findById(ticketId);
        if (ticket == null) throw new MaintenanceTicketNotFoundException("Ticket " + ticketId + " not found");

        ticket.status = newStatus;
        if (newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED) {
            ticket.resolvedAt = LocalDateTime.now();
        }

        if (newStatus == TicketStatus.OPEN || newStatus == TicketStatus.IN_PROGRESS) {
            ticket.resolvedAt = null;
        }


        // Business rule: once no open ticket remains, the equipment goes back to operational
        List<MaintenanceTicket> stillOpen = MaintenanceTicket.findOpenByEquipment(ticket.equipment.id);
        if (stillOpen.isEmpty() && ticket.equipment.status == EquipmentStatus.IN_MAINTENANCE) {
            ticket.equipment.status = EquipmentStatus.OPERATIONAL;
        }

        return ticket;
    }

    @Transactional
    public MaintenanceTicketDTO updateTicket(UUID ticketId, UpdateTicketRequest request) {
        if (ticketId == null) throw new IllegalArgumentException("Ticket ID cannot be null");
        MaintenanceTicket ticket = MaintenanceTicket.findById(ticketId);

        if (ticket == null) {
            throw new MaintenanceTicketNotFoundException("Ticket " + ticketId + " not found");
        }
        if (request.title() != null) ticket.title = request.title();
        if (request.description() != null) ticket.description = request.description();
        if (request.priority() != null) ticket.priority = request.priority();
        if (request.assignedTo() != null) ticket.assignedTo = request.assignedTo();
        return MaintenanceTicketDTO.from(ticket);
    }

    @Transactional
    public boolean deleteTicket(UUID ticketId) {
        if (ticketId == null) throw new IllegalArgumentException("Ticket ID cannot be null");
        return MaintenanceTicket.deleteById(ticketId);
    }

}
