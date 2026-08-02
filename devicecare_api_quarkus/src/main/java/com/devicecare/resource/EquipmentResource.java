package com.devicecare.resource;

import com.devicecare.dto.EquipmentDTO;
import com.devicecare.dto.EquipmentRequest;
import com.devicecare.entity.enums.EquipmentStatus;
import com.devicecare.service.EquipmentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.UUID;

@Path("/equipments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EquipmentResource {

    @Inject
    EquipmentService equipmentService;

    // GET endpoint for listing equipments
    @GET
    public Response listEquipments(@QueryParam("status") EquipmentStatus status,
                                   @QueryParam("type") String type) {
        return Response.
                ok(equipmentService.getAllEquipments(status, type))
                .build();
    }

    // GET endpoint for retrieving a specific equipment by ID
    @GET
    @Path("/{id}")
    public Response getEquipment(@PathParam("id") UUID id) {
        return Response
                .ok(equipmentService.getEquipmentById(id))
                .build();
    }

    // POST endpoint for creating a new equipment
    @POST
    public Response create(@Valid EquipmentRequest request) {
        EquipmentDTO equipment = equipmentService.createEquipment(request);
        return Response.created(URI.create("/equipments/" + equipment.id))
                .entity(equipment)
                .build();
    }

    // PUT endpoint for updating an existing equipment
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") UUID id, @Valid EquipmentRequest request) {
        return Response
                .ok(equipmentService.updateEquipment(id, request))
                .build();
    }

    // DELETE endpoint for deleting an equipment
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        boolean deleted = equipmentService.deleteEquipment(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}