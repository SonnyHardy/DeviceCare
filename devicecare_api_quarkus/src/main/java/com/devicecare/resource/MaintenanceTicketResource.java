package com.devicecare.resource;

import com.devicecare.dto.CreateTicketRequest;
import com.devicecare.dto.MaintenanceTicketDTO;
import com.devicecare.dto.UpdateTicketRequest;
import com.devicecare.entity.MaintenanceTicket;
import com.devicecare.entity.enums.TicketPriority;
import com.devicecare.entity.enums.TicketStatus;
import com.devicecare.service.MaintenanceTicketService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.UUID;

@Path("/tickets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaintenanceTicketResource {

    @Inject
    MaintenanceTicketService ticketService;

    // GET endpoint for listing tickets
    @GET
    public Response listTickets(@QueryParam("equipmentId") UUID equipmentId,
                                           @QueryParam("status") TicketStatus status,
                                           @QueryParam("priority") TicketPriority priority) {
        return Response
                .ok(ticketService.listTickets(equipmentId, status, priority))
                .build();
    }

    // GET endpoint for retrieving a specific ticket by ID
    @GET
    @Path("/{id}")
    public Response getTicket(@PathParam("id") UUID id) {
        MaintenanceTicketDTO ticket = ticketService.getTicketById(id);
        return Response.ok(ticket).build();
    }

    // POST endpoint for creating a new ticket
    @POST
    public Response create(@Valid CreateTicketRequest request) {
        MaintenanceTicket ticket = ticketService.createTicket(request);
        return Response.created(URI.create("/tickets/" + ticket.id))
                .entity(MaintenanceTicketDTO.from(ticket))
                .build();
    }

    // Dedicated endpoint for status transitions
    @PATCH
    @Path("/{id}/status")
    public Response updateStatus(@PathParam("id") UUID id, @NotNull TicketStatus newStatus) {
        MaintenanceTicket ticket = ticketService.updateTicketStatus(id, newStatus);
        return Response.ok(MaintenanceTicketDTO.from(ticket)).build();
    }

    // PUT endpoint for updating ticket details
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") UUID id, @Valid UpdateTicketRequest request) {
        MaintenanceTicketDTO ticket = ticketService.updateTicket(id, request);
        return Response.ok(ticket).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        boolean deleted = ticketService.deleteTicket(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}