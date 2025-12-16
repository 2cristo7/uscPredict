package usc.uscPredict.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.fge.jsonpatch.JsonPatchException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.model.Event;
import usc.uscPredict.model.EventState;
import usc.uscPredict.service.EventService;
import usc.uscPredict.util.PatchUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for Event endpoints.
 * Handles HTTP requests for event management.
 * Base path: /api/v1/events
 */
@Tag(name = "Events", description = "API de gestión de eventos de predicción")
@RestController
@RequestMapping("/api/v1/events")
@Validated
public class EventController {

    private final EventService eventService;
    private final PatchUtils patchUtils;

    @Autowired
    public EventController(EventService eventService, PatchUtils patchUtils) {
        this.eventService = eventService;
        this.patchUtils = patchUtils;
    }

    /**
     * GET /events
     * Retrieves all events.
     * @return 200 OK with list of events
     */
    @Operation(
            summary = "Listar todos los eventos",
            description = "Obtiene una lista completa de todos los eventos de predicción en la plataforma"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos obtenida exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Event.class)))
    })
    @GetMapping
    @JsonView(Event.EventSummaryView.class)
    public ResponseEntity<Set<Event>> getAllEvents() {
        Set<Event> events = eventService.getAllEvents();
        return new ResponseEntity<>(events, HttpStatus.OK);
    }

    /**
     * GET /events/{uuid}
     * Retrieves a single event by UUID.
     * @param uuid The event UUID
     * @return 200 OK with event, or 404 NOT FOUND
     */
    @Operation(
            summary = "Obtener evento por ID",
            description = "Retorna los detalles completos de un evento específico identificado por su UUID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento encontrado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Event.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content)
    })
    @GetMapping("/{uuid}")
    @JsonView(Event.EventDetailView.class)
    public ResponseEntity<Event> getEventById(
            @Parameter(description = "UUID del evento a buscar", required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
        Event event = eventService.getEventById(uuid);
        return ResponseEntity.ok(event);
    }

    /**
     * POST /events
     * Creates a new event.
     * @param event The event data (JSON body)
     * @return 201 CREATED with created event, or 400 BAD REQUEST
     */
    @Operation(
            summary = "Crear nuevo evento",
            description = "Crea un nuevo evento de predicción. Requiere título, descripción, fechas de inicio/fin " +
                    "y el usuario creador. El estado inicial será ACTIVE"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Evento creado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Event.class))),
            @ApiResponse(responseCode = "400", description = "Datos del evento inválidos", content = @Content)
    })
    @PostMapping
    @JsonView(Event.EventDetailView.class)
    public ResponseEntity<Event> createEvent(@RequestBody @Valid @NonNull Event event) {
        Event created = eventService.createEvent(event);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * PUT /events/{uuid}
     * Updates an existing event.
     * @param uuid The event UUID
     * @param event The updated event data
     * @return 200 OK with updated event, or 404 NOT FOUND
     */
    @Operation(
            summary = "Actualizar evento completo",
            description = "Actualiza todos los campos de un evento existente mediante reemplazo completo (PUT). " +
                    "Use PATCH para actualizaciones parciales"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento actualizado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Event.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Datos del evento inválidos", content = @Content)
    })
    @PutMapping("/{uuid}")
    @JsonView(Event.EventDetailView.class)
    public ResponseEntity<Event> updateEvent(
            @Parameter(description = "UUID del evento a actualizar", required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid,
            @RequestBody @Valid @NonNull Event event) {
        Event updated = eventService.updateEvent(uuid, event);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /events/{uuid}
     * Deletes an event.
     * @param uuid The event UUID
     * @return 204 NO CONTENT if deleted, or 404 NOT FOUND
     */
    @Operation(
            summary = "Eliminar evento",
            description = "Elimina permanentemente un evento de la plataforma. " +
                    "ADVERTENCIA: Esta acción no se puede deshacer"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Evento eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "UUID del evento a eliminar", required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
        boolean deleted = eventService.deleteEvent(uuid);
        if (deleted) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * GET /events/state/{state}
     * Retrieves all events with a specific state.
     * Example: GET /events/state/OPEN
     * @param state The event state (OPEN, CLOSED, SETTLED)
     * @return 200 OK with list of events
     */
    @Operation(
            summary = "Buscar eventos por estado",
            description = "Retorna todos los eventos filtrados por su estado actual " +
                    "(ACTIVE, CLOSED, RESOLVED)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos filtrada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Event.class)))
    })
    @GetMapping("/state/{state}")
    @JsonView(Event.EventSummaryView.class)
    public ResponseEntity<Set<Event>> getEventsByState(
            @Parameter(description = "Estado del evento (ACTIVE, CLOSED, RESOLVED)", required = true,
                    example = "ACTIVE")
            @PathVariable EventState state) {
        Set<Event> events = eventService.getEventsByState(state);
        return new ResponseEntity<>(events, HttpStatus.OK);
    }

    /**
     * PATCH /events/{uuid}
     * Applies JSON-Patch operations to an event (RFC 6902).
     * Body: [{ "op": "replace", "path": "/state", "value": "CLOSED" }]
     * @param uuid The event UUID
     * @param updates List of JSON-Patch operations
     * @return 200 OK with updated event, 404 NOT FOUND, or 400 BAD REQUEST
     */
    @Operation(
            summary = "Actualizar evento parcialmente con JSON-Patch",
            description = "Aplica operaciones JSON-Patch (RFC 6902) para actualizar campos específicos de un evento. " +
                    "Permite modificar estado, título, descripción, fechas, etc."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento actualizado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Event.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Operación de patch inválida", content = @Content)
    })
    @PatchMapping("/{uuid}")
    @JsonView(Event.EventDetailView.class)
    public ResponseEntity<Event> patchEvent(
            @Parameter(description = "UUID del evento a actualizar", required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid,
            @RequestBody List<Map<String, Object>> updates) throws JsonPatchException {
        // 1. Obter o evento da base de datos (throws exception if not found)
        Event existingEvent = eventService.getEventById(uuid);

        // 2. Aplicar as operacións JSON-Patch (JsonPatchException handled globally)
        Event patchedEvent = patchUtils.applyPatch(existingEvent, updates);

        // 3. Gardar o recurso actualizado
        Event updated = eventService.updateEvent(uuid, patchedEvent);
        return ResponseEntity.ok(updated);
    }
}
