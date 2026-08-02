package com.devicecare.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.devicecare.entity.enums.TicketPriority;
import com.devicecare.entity.enums.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@Table(name = "maintenance_ticket")
public class MaintenanceTicket extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    public Equipment equipment;

    @Column(nullable = false)
    public String title;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TicketPriority priority = TicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TicketStatus status = TicketStatus.OPEN;

    @Column(name = "assigned_to")
    public String assignedTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "resolved_at")
    public LocalDateTime resolvedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }


    public static List<MaintenanceTicket> findByEquipment(UUID equipmentId) {
        return list("equipment.id", equipmentId);
    }

    // "Open" here means OPEN or IN_PROGRESS, used by the business rule that
    // flips the equipment back to OPERATIONAL once no open ticket remains.
    public static List<MaintenanceTicket> findOpenByEquipment(UUID equipmentId) {
        return list("equipment.id = ?1 and status in ?2", equipmentId,
                List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS));
    }
}