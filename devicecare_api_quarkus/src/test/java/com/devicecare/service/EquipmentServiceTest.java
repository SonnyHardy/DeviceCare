package com.devicecare.service;

import com.devicecare.dto.EquipmentDTO;
import com.devicecare.dto.EquipmentRequest;
import com.devicecare.entity.Equipment;
import com.devicecare.entity.enums.EquipmentStatus;
import com.devicecare.exception.EquipmentNotFoundException;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class EquipmentServiceTest {

    private final EquipmentService service = new EquipmentService();

    @BeforeEach
    void setUp() {
        PanacheMock.mock(Equipment.class);
    }

    @Test
    void shouldListEquipmentFilteredByStatusAndType() {
        Equipment equipment = equipment("Pump", "Hydraulic", EquipmentStatus.OPERATIONAL);
        Mockito.when(Equipment.list("status = ?1 and type = ?2", EquipmentStatus.OPERATIONAL, "Hydraulic"))
                .thenReturn(List.of(equipment));

        List<EquipmentDTO> result = service.getAllEquipments(EquipmentStatus.OPERATIONAL, "Hydraulic");

        assertEquals(1, result.size());
        assertEquipmentDtoMatches(equipment, result.getFirst());
        PanacheMock.verify(Equipment.class).list(
                "status = ?1 and type = ?2", EquipmentStatus.OPERATIONAL, "Hydraulic");
    }

    @Test
    void shouldListEquipmentFilteredByStatusOnly() {
        Equipment equipment = equipment("Pump", "Hydraulic", EquipmentStatus.DEFECTIVE);
        Mockito.when(Equipment.list("status", EquipmentStatus.DEFECTIVE)).thenReturn(List.of(equipment));

        List<EquipmentDTO> result = service.getAllEquipments(EquipmentStatus.DEFECTIVE, null);

        assertEquals(1, result.size());
        assertEquipmentDtoMatches(equipment, result.getFirst());
        PanacheMock.verify(Equipment.class).list("status", EquipmentStatus.DEFECTIVE);
    }

    @Test
    void shouldListEquipmentFilteredByTypeOnly() {
        Equipment equipment = equipment("Pump", "Hydraulic", EquipmentStatus.OPERATIONAL);
        Mockito.when(Equipment.list("type", "Hydraulic")).thenReturn(List.of(equipment));

        List<EquipmentDTO> result = service.getAllEquipments(null, "Hydraulic");

        assertEquals(1, result.size());
        assertEquipmentDtoMatches(equipment, result.getFirst());
        PanacheMock.verify(Equipment.class).list("type", "Hydraulic");
    }

    @Test
    void shouldListAllEquipmentWhenNoFilterIsProvided() {
        Equipment first = equipment("Pump", "Hydraulic", EquipmentStatus.OPERATIONAL);
        Equipment second = equipment("Generator", "Electrical", EquipmentStatus.IN_MAINTENANCE);
        Mockito.when(Equipment.listAll()).thenReturn(List.of(first, second));

        List<EquipmentDTO> result = service.getAllEquipments(null, null);

        assertEquals(2, result.size());
        assertEquipmentDtoMatches(first, result.get(0));
        assertEquipmentDtoMatches(second, result.get(1));
        PanacheMock.verify(Equipment.class).listAll();
    }

    @Test
    void shouldReturnAnEmptyListWhenNoEquipmentMatches() {
        Mockito.when(Equipment.listAll()).thenReturn(List.of());

        assertTrue(service.getAllEquipments(null, null).isEmpty());
    }

    @Test
    void shouldReturnEquipmentById() {
        Equipment equipment = equipment("Pump", "Hydraulic", EquipmentStatus.OPERATIONAL);
        Mockito.when(Equipment.findById(equipment.id)).thenReturn(equipment);

        EquipmentDTO result = service.getEquipmentById(equipment.id);

        assertEquipmentDtoMatches(equipment, result);
    }

    @Test
    void shouldThrowWhenEquipmentDoesNotExist() {
        UUID id = UUID.randomUUID();
        Mockito.when(Equipment.findById(id)).thenReturn(null);

        EquipmentNotFoundException exception = assertThrows(
                EquipmentNotFoundException.class, () -> service.getEquipmentById(id));

        assertEquals("Equipment " + id + " not found", exception.getMessage());
    }

    @Test
    void shouldCreateAndPersistEquipment() {
        LocalDate purchaseDate = LocalDate.of(2025, 3, 14);
        EquipmentRequest request = new EquipmentRequest(
                "Pump", "Hydraulic", "SN-001", "Building A", purchaseDate);

        try (MockedConstruction<Equipment> construction = Mockito.mockConstruction(
                Equipment.class,
                Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS),
                (mock, context) -> Mockito.doNothing().when(mock).persist())) {
            EquipmentDTO result = service.createEquipment(request);
            Equipment persistedEquipment = construction.constructed().getFirst();

            assertEquals(request.name(), result.name);
            assertEquals(request.type(), result.type);
            assertEquals(request.serialNumber(), result.serialNumber);
            assertEquals(request.location(), result.location);
            assertEquals(request.purchaseDate(), result.purchaseDate);
            Mockito.verify(persistedEquipment).persist();
        }
    }

    @Test
    void shouldUpdateEveryEditableEquipmentField() {
        Equipment equipment = equipment("Old name", "Old type", EquipmentStatus.OPERATIONAL);
        LocalDate purchaseDate = LocalDate.of(2026, 1, 5);
        EquipmentRequest request = new EquipmentRequest(
                "New name", "New type", "NEW-SERIAL", "Building B", purchaseDate);
        Mockito.when(Equipment.findById(equipment.id)).thenReturn(equipment);

        EquipmentDTO result = service.updateEquipment(equipment.id, request);

        assertEquals("New name", equipment.name);
        assertEquals("New type", equipment.type);
        assertEquals("NEW-SERIAL", equipment.serialNumber);
        assertEquals("Building B", equipment.location);
        assertEquals(purchaseDate, equipment.purchaseDate);
        assertEquipmentDtoMatches(equipment, result);
    }

    @Test
    void shouldRejectNullEquipmentIdWhenUpdating() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateEquipment(null, new EquipmentRequest("Name", "Type", "SN", null, null)));

        assertEquals("Equipment ID cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowWhenUpdatingUnknownEquipment() {
        UUID id = UUID.randomUUID();
        Mockito.when(Equipment.findById(id)).thenReturn(null);

        EquipmentNotFoundException exception = assertThrows(
                EquipmentNotFoundException.class,
                () -> service.updateEquipment(id, new EquipmentRequest("Name", "Type", "SN", null, null)));

        assertEquals("Equipment " + id + " not found", exception.getMessage());
    }

    @Test
    void shouldDeleteEquipmentAndReturnTrueWhenItExists() {
        UUID id = UUID.randomUUID();
        Mockito.when(Equipment.deleteById(id)).thenReturn(true);

        assertTrue(service.deleteEquipment(id));
        PanacheMock.verify(Equipment.class).deleteById(id);
    }

    @Test
    void shouldReturnFalseWhenDeletingUnknownEquipment() {
        UUID id = UUID.randomUUID();
        Mockito.when(Equipment.deleteById(id)).thenReturn(false);

        assertFalse(service.deleteEquipment(id));
    }

    @Test
    void shouldRejectNullEquipmentIdWhenDeleting() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.deleteEquipment(null));

        assertEquals("Equipment ID cannot be null", exception.getMessage());
    }

    private static Equipment equipment(String name, String type, EquipmentStatus status) {
        Equipment equipment = new Equipment();
        equipment.id = UUID.randomUUID();
        equipment.name = name;
        equipment.type = type;
        equipment.serialNumber = "SN-" + equipment.id;
        equipment.status = status;
        equipment.location = "Building A";
        equipment.purchaseDate = LocalDate.of(2024, 6, 1);
        return equipment;
    }

    private static void assertEquipmentDtoMatches(Equipment equipment, EquipmentDTO dto) {
        assertAll(
                () -> assertEquals(equipment.id, dto.id),
                () -> assertEquals(equipment.name, dto.name),
                () -> assertEquals(equipment.type, dto.type),
                () -> assertEquals(equipment.serialNumber, dto.serialNumber),
                () -> assertEquals(equipment.status, dto.status),
                () -> assertEquals(equipment.location, dto.location),
                () -> assertEquals(equipment.purchaseDate, dto.purchaseDate));
    }
}
