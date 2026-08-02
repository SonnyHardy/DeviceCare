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
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MaintenanceTicketServiceTest {

    private final MaintenanceTicketService service = new MaintenanceTicketService();

    @BeforeEach
    void setUp() {
        PanacheMock.mock(Equipment.class);
        PanacheMock.mock(MaintenanceTicket.class);
    }

    @Test
    void shouldListAllTicketsWhenNoFilterIsProvided() {
        MaintenanceTicket first = ticket(TicketStatus.OPEN, TicketPriority.HIGH);
        MaintenanceTicket second = ticket(TicketStatus.CLOSED, TicketPriority.LOW);
        Mockito.when(MaintenanceTicket.listAll()).thenReturn(List.of(first, second));

        List<MaintenanceTicketDTO> result = service.listTickets(null, null, null);

        assertEquals(2, result.size());
        assertTicketDtoMatches(first, result.get(0));
        assertTicketDtoMatches(second, result.get(1));
        PanacheMock.verify(MaintenanceTicket.class).listAll();
    }

    @Test
    void shouldListTicketsForOneEquipment() {
        MaintenanceTicket matchingTicket = ticket(TicketStatus.OPEN, TicketPriority.MEDIUM);
        UUID equipmentId = matchingTicket.equipment.id;
        Mockito.when(MaintenanceTicket.findByEquipment(equipmentId)).thenReturn(List.of(matchingTicket));

        List<MaintenanceTicketDTO> result = service.listTickets(equipmentId, null, null);

        assertEquals(1, result.size());
        assertTicketDtoMatches(matchingTicket, result.getFirst());
        PanacheMock.verify(MaintenanceTicket.class).findByEquipment(equipmentId);
    }

    @Test
    void shouldFilterTicketsByStatus() {
        MaintenanceTicket matching = ticket(TicketStatus.IN_PROGRESS, TicketPriority.HIGH);
        MaintenanceTicket excluded = ticket(TicketStatus.OPEN, TicketPriority.HIGH);
        Mockito.when(MaintenanceTicket.listAll()).thenReturn(List.of(matching, excluded));

        List<MaintenanceTicketDTO> result = service.listTickets(null, TicketStatus.IN_PROGRESS, null);

        assertEquals(1, result.size());
        assertEquals(matching.id, result.getFirst().id);
    }

    @Test
    void shouldFilterTicketsByPriority() {
        MaintenanceTicket matching = ticket(TicketStatus.OPEN, TicketPriority.CRITICAL);
        MaintenanceTicket excluded = ticket(TicketStatus.OPEN, TicketPriority.LOW);
        Mockito.when(MaintenanceTicket.listAll()).thenReturn(List.of(matching, excluded));

        List<MaintenanceTicketDTO> result = service.listTickets(null, null, TicketPriority.CRITICAL);

        assertEquals(1, result.size());
        assertEquals(matching.id, result.getFirst().id);
    }

    @Test
    void shouldApplyEquipmentStatusAndPriorityFiltersTogether() {
        MaintenanceTicket matching = ticket(TicketStatus.RESOLVED, TicketPriority.HIGH);
        MaintenanceTicket wrongStatus = ticket(TicketStatus.OPEN, TicketPriority.HIGH);
        MaintenanceTicket wrongPriority = ticket(TicketStatus.RESOLVED, TicketPriority.LOW);
        UUID equipmentId = matching.equipment.id;
        Mockito.when(MaintenanceTicket.findByEquipment(equipmentId))
                .thenReturn(List.of(matching, wrongStatus, wrongPriority));

        List<MaintenanceTicketDTO> result = service.listTickets(
                equipmentId, TicketStatus.RESOLVED, TicketPriority.HIGH);

        assertEquals(1, result.size());
        assertEquals(matching.id, result.getFirst().id);
    }

    @Test
    void shouldReturnAnEmptyListWhenNoTicketMatches() {
        Mockito.when(MaintenanceTicket.listAll()).thenReturn(List.of());

        assertTrue(service.listTickets(null, TicketStatus.OPEN, TicketPriority.LOW).isEmpty());
    }

    @Test
    void shouldReturnTicketById() {
        MaintenanceTicket ticket = ticket(TicketStatus.OPEN, TicketPriority.MEDIUM);
        Mockito.when(MaintenanceTicket.findById(ticket.id)).thenReturn(ticket);

        MaintenanceTicketDTO result = service.getTicketById(ticket.id);

        assertTicketDtoMatches(ticket, result);
    }

    @Test
    void shouldRejectNullTicketIdWhenGettingTicket() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.getTicketById(null));

        assertEquals("Ticket ID cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowWhenTicketDoesNotExist() {
        UUID id = UUID.randomUUID();
        Mockito.when(MaintenanceTicket.findById(id)).thenReturn(null);

        MaintenanceTicketNotFoundException exception = assertThrows(
                MaintenanceTicketNotFoundException.class, () -> service.getTicketById(id));

        assertEquals("Ticket " + id + " not found", exception.getMessage());
    }

    @Test
    void shouldCreateTicketWithExplicitPriorityAndPutOperationalEquipmentInMaintenance() {
        Equipment equipment = equipment(EquipmentStatus.OPERATIONAL);
        CreateTicketRequest request = new CreateTicketRequest(
                UUID.randomUUID(), "Leaking pump", "Seal must be replaced", TicketPriority.CRITICAL, "Alex");
        Mockito.when(Equipment.findById(request.equipmentId())).thenReturn(equipment);

        try (MockedConstruction<MaintenanceTicket> ignored = Mockito.mockConstruction(
                MaintenanceTicket.class,
                Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS),
                (mock, context) -> Mockito.doNothing().when(mock).persist())) {
            MaintenanceTicket result = service.createTicket(request);

            assertAll(
                    () -> assertSame(equipment, result.equipment),
                    () -> assertEquals(request.title(), result.title),
                    () -> assertEquals(request.description(), result.description),
                    () -> assertEquals(TicketPriority.CRITICAL, result.priority),
                    () -> assertEquals(request.assignedTo(), result.assignedTo),
                    () -> assertEquals(EquipmentStatus.IN_MAINTENANCE, equipment.status));
            Mockito.verify(result).persist();
        }
    }

    @Test
    void shouldKeepDefaultPriorityWhenCreatingTicketWithoutPriority() {
        Equipment equipment = equipment(EquipmentStatus.DEFECTIVE);
        CreateTicketRequest request = new CreateTicketRequest(UUID.randomUUID(), "Inspection", null, null, null);
        Mockito.when(Equipment.findById(request.equipmentId())).thenReturn(equipment);

        try (MockedConstruction<MaintenanceTicket> ignored = Mockito.mockConstruction(
                MaintenanceTicket.class,
                Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS),
                (mock, context) -> {
                    mock.priority = TicketPriority.MEDIUM;
                    Mockito.doNothing().when(mock).persist();
                })) {
            MaintenanceTicket result = service.createTicket(request);

            assertEquals(TicketPriority.MEDIUM, result.priority);
            assertEquals(EquipmentStatus.DEFECTIVE, equipment.status);
            Mockito.verify(result).persist();
        }
    }

    @Test
    void shouldThrowWhenCreatingTicketForUnknownEquipment() {
        CreateTicketRequest request = new CreateTicketRequest(UUID.randomUUID(), "Inspection", null, null, null);
        Mockito.when(Equipment.findById(request.equipmentId())).thenReturn(null);

        EquipmentNotFoundException exception = assertThrows(
                EquipmentNotFoundException.class, () -> service.createTicket(request));

        assertEquals("Equipment " + request.equipmentId() + " not found", exception.getMessage());
    }

    @Test
    void shouldSetResolutionTimeWhenResolvingTicket() {
        MaintenanceTicket ticket = ticket(TicketStatus.IN_PROGRESS, TicketPriority.HIGH);
        Mockito.when(MaintenanceTicket.findById(ticket.id)).thenReturn(ticket);
        Mockito.when(MaintenanceTicket.findOpenByEquipment(ticket.equipment.id)).thenReturn(List.of(ticket));
        LocalDateTime beforeUpdate = LocalDateTime.now();

        MaintenanceTicket result = service.updateTicketStatus(ticket.id, TicketStatus.RESOLVED);

        assertSame(ticket, result);
        assertEquals(TicketStatus.RESOLVED, ticket.status);
        assertNotNull(ticket.resolvedAt);
        assertFalse(ticket.resolvedAt.isBefore(beforeUpdate));
    }

    @Test
    void shouldSetResolutionTimeWhenClosingTicket() {
        MaintenanceTicket ticket = ticket(TicketStatus.OPEN, TicketPriority.MEDIUM);
        Mockito.when(MaintenanceTicket.findById(ticket.id)).thenReturn(ticket);
        Mockito.when(MaintenanceTicket.findOpenByEquipment(ticket.equipment.id)).thenReturn(List.of(ticket));

        service.updateTicketStatus(ticket.id, TicketStatus.CLOSED);

        assertEquals(TicketStatus.CLOSED, ticket.status);
        assertNotNull(ticket.resolvedAt);
    }

    @Test
    void shouldClearResolutionTimeWhenReopeningTicket() {
        MaintenanceTicket ticket = ticket(TicketStatus.RESOLVED, TicketPriority.MEDIUM);
        ticket.resolvedAt = LocalDateTime.now().minusDays(1);
        Mockito.when(MaintenanceTicket.findById(ticket.id)).thenReturn(ticket);
        Mockito.when(MaintenanceTicket.findOpenByEquipment(ticket.equipment.id)).thenReturn(List.of(ticket));

        service.updateTicketStatus(ticket.id, TicketStatus.OPEN);

        assertEquals(TicketStatus.OPEN, ticket.status);
        assertNull(ticket.resolvedAt);
    }

    @Test
    void shouldClearResolutionTimeWhenMovingTicketBackToInProgress() {
        MaintenanceTicket ticket = ticket(TicketStatus.CLOSED, TicketPriority.MEDIUM);
        ticket.resolvedAt = LocalDateTime.now().minusDays(1);
        Mockito.when(MaintenanceTicket.findById(ticket.id)).thenReturn(ticket);
        Mockito.when(MaintenanceTicket.findOpenByEquipment(ticket.equipment.id)).thenReturn(List.of(ticket));

        service.updateTicketStatus(ticket.id, TicketStatus.IN_PROGRESS);

        assertEquals(TicketStatus.IN_PROGRESS, ticket.status);
        assertNull(ticket.resolvedAt);
    }

    @Test
    void shouldRestoreEquipmentToOperationalWhenNoOpenTicketRemains() {
        MaintenanceTicket ticket = ticket(TicketStatus.IN_PROGRESS, TicketPriority.HIGH);
        ticket.equipment.status = EquipmentStatus.IN_MAINTENANCE;
        Mockito.when(MaintenanceTicket.findById(ticket.id)).thenReturn(ticket);
        Mockito.when(MaintenanceTicket.findOpenByEquipment(ticket.equipment.id)).thenReturn(List.of());

        service.updateTicketStatus(ticket.id, TicketStatus.RESOLVED);

        assertEquals(EquipmentStatus.OPERATIONAL, ticket.equipment.status);
    }

    @Test
    void shouldKeepEquipmentInMaintenanceWhileAnotherOpenTicketRemains() {
        MaintenanceTicket ticket = ticket(TicketStatus.IN_PROGRESS, TicketPriority.HIGH);
        ticket.equipment.status = EquipmentStatus.IN_MAINTENANCE;
        MaintenanceTicket otherOpenTicket = ticket(TicketStatus.OPEN, TicketPriority.LOW);
        Mockito.when(MaintenanceTicket.findById(ticket.id)).thenReturn(ticket);
        Mockito.when(MaintenanceTicket.findOpenByEquipment(ticket.equipment.id))
                .thenReturn(List.of(otherOpenTicket));

        service.updateTicketStatus(ticket.id, TicketStatus.RESOLVED);

        assertEquals(EquipmentStatus.IN_MAINTENANCE, ticket.equipment.status);
    }

    @Test
    void shouldNotChangeNonMaintenanceEquipmentWhenNoOpenTicketRemains() {
        MaintenanceTicket ticket = ticket(TicketStatus.OPEN, TicketPriority.LOW);
        ticket.equipment.status = EquipmentStatus.DEFECTIVE;
        Mockito.when(MaintenanceTicket.findById(ticket.id)).thenReturn(ticket);
        Mockito.when(MaintenanceTicket.findOpenByEquipment(ticket.equipment.id)).thenReturn(List.of());

        service.updateTicketStatus(ticket.id, TicketStatus.CLOSED);

        assertEquals(EquipmentStatus.DEFECTIVE, ticket.equipment.status);
    }

    @Test
    void shouldRejectNullTicketIdWhenUpdatingStatus() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateTicketStatus(null, TicketStatus.RESOLVED));

        assertEquals("Ticket ID cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowWhenUpdatingStatusOfUnknownTicket() {
        UUID id = UUID.randomUUID();
        Mockito.when(MaintenanceTicket.findById(id)).thenReturn(null);

        MaintenanceTicketNotFoundException exception = assertThrows(
                MaintenanceTicketNotFoundException.class,
                () -> service.updateTicketStatus(id, TicketStatus.RESOLVED));

        assertEquals("Ticket " + id + " not found", exception.getMessage());
    }

    @Test
    void shouldUpdateEveryProvidedTicketField() {
        MaintenanceTicket ticket = ticket(TicketStatus.OPEN, TicketPriority.LOW);
        UpdateTicketRequest request = new UpdateTicketRequest(
                "New title", "New description", TicketPriority.CRITICAL, "Morgan");
        Mockito.when(MaintenanceTicket.findById(ticket.id)).thenReturn(ticket);

        MaintenanceTicketDTO result = service.updateTicket(ticket.id, request);

        assertAll(
                () -> assertEquals("New title", ticket.title),
                () -> assertEquals("New description", ticket.description),
                () -> assertEquals(TicketPriority.CRITICAL, ticket.priority),
                () -> assertEquals("Morgan", ticket.assignedTo));
        assertTicketDtoMatches(ticket, result);
    }

    @Test
    void shouldLeaveTicketFieldsUnchangedWhenUpdateValuesAreNull() {
        MaintenanceTicket ticket = ticket(TicketStatus.OPEN, TicketPriority.HIGH);
        ticket.description = "Original description";
        ticket.assignedTo = "Taylor";
        Mockito.when(MaintenanceTicket.findById(ticket.id)).thenReturn(ticket);

        service.updateTicket(ticket.id, new UpdateTicketRequest(null, null, null, null));

        assertAll(
                () -> assertEquals("Ticket title", ticket.title),
                () -> assertEquals("Original description", ticket.description),
                () -> assertEquals(TicketPriority.HIGH, ticket.priority),
                () -> assertEquals("Taylor", ticket.assignedTo));
    }

    @Test
    void shouldRejectNullTicketIdWhenUpdatingTicket() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateTicket(null, new UpdateTicketRequest(null, null, null, null)));

        assertEquals("Ticket ID cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowWhenUpdatingUnknownTicket() {
        UUID id = UUID.randomUUID();
        Mockito.when(MaintenanceTicket.findById(id)).thenReturn(null);

        MaintenanceTicketNotFoundException exception = assertThrows(
                MaintenanceTicketNotFoundException.class,
                () -> service.updateTicket(id, new UpdateTicketRequest(null, null, null, null)));

        assertEquals("Ticket " + id + " not found", exception.getMessage());
    }

    @Test
    void shouldDeleteTicketAndReturnTrueWhenItExists() {
        UUID id = UUID.randomUUID();
        Mockito.when(MaintenanceTicket.deleteById(id)).thenReturn(true);

        assertTrue(service.deleteTicket(id));
        PanacheMock.verify(MaintenanceTicket.class).deleteById(id);
    }

    @Test
    void shouldReturnFalseWhenDeletingUnknownTicket() {
        UUID id = UUID.randomUUID();
        Mockito.when(MaintenanceTicket.deleteById(id)).thenReturn(false);

        assertFalse(service.deleteTicket(id));
    }

    @Test
    void shouldRejectNullTicketIdWhenDeleting() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.deleteTicket(null));

        assertEquals("Ticket ID cannot be null", exception.getMessage());
    }

    private static Equipment equipment(EquipmentStatus status) {
        Equipment equipment = new Equipment();
        equipment.id = UUID.randomUUID();
        equipment.status = status;
        return equipment;
    }

    private static MaintenanceTicket ticket(TicketStatus status, TicketPriority priority) {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.id = UUID.randomUUID();
        ticket.equipment = equipment(EquipmentStatus.IN_MAINTENANCE);
        ticket.title = "Ticket title";
        ticket.description = "Ticket description";
        ticket.status = status;
        ticket.priority = priority;
        ticket.assignedTo = "Jordan";
        ticket.createdAt = LocalDateTime.of(2026, 2, 1, 10, 30);
        return ticket;
    }

    private static void assertTicketDtoMatches(MaintenanceTicket ticket, MaintenanceTicketDTO dto) {
        assertAll(
                () -> assertEquals(ticket.id, dto.id),
                () -> assertEquals(ticket.equipment.id, dto.equipmentId),
                () -> assertEquals(ticket.title, dto.title),
                () -> assertEquals(ticket.description, dto.description),
                () -> assertEquals(ticket.priority, dto.priority),
                () -> assertEquals(ticket.status, dto.status),
                () -> assertEquals(ticket.assignedTo, dto.assignedTo),
                () -> assertEquals(ticket.createdAt, dto.createdAt),
                () -> assertEquals(ticket.resolvedAt, dto.resolvedAt));
    }
}
