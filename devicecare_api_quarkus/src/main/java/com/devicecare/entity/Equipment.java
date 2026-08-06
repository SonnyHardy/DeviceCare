package com.devicecare.entity;

import com.devicecare.entity.enums.EquipmentStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "equipment")
public class Equipment extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String type;

    @Column(name= "serial_number", nullable = false, unique = true)
    public String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EquipmentStatus status = EquipmentStatus.OPERATIONAL;

    public String location;

    @Column(name = "purchase_date")
    public LocalDate purchaseDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<MaintenanceTicket> tickets;


    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // query helpers live on the entity itself
    public static Equipment findBySerialNumber(String serialNumber) {
        return find("serialNumber", serialNumber).firstResult();
    }

}
