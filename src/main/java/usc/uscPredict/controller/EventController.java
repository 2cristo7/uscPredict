package usc.uscPredict.controller;

import com.github.fge.jsonpatch.JsonPatchException;
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
 * Base path: /events
 */
@RestController
@RequestMapping("/events")
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
    @GetMapping
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
    @GetMapping("/{uuid}")
    public ResponseEntity<Event> getEventById(@PathVariable UUID uuid) {
        Event event = eventService.getEventById(uuid);
        if (event != null) {
            return new ResponseEntity<>(event, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * POST /events
     * Creates a new event.
     * @param event The event data (JSON body)
     * @return 201 CREATED with created event, or 400 BAD REQUEST
     */
    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody @Valid @NonNull Event event) {
        Event created = eventService.createEvent(event);
        if (created != null) {
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * PUT /events/{uuid}
     * Updates an existing event.
     * @param uuid The event UUID
     * @param event The updated event data
     * @return 200 OK with updated event, or 404 NOT FOUND
     */
    @PutMapping("/{uuid}")
    public ResponseEntity<Event> updateEvent(
            @PathVariable UUID uuid,
            @RequestBody @Valid @NonNull Event event) {
        Event updated = eventService.updateEvent(uuid, event);
        if (updated != null) {
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * DELETE /events/{uuid}
     * Deletes an event.
     * @param uuid The event UUID
     * @return 204 NO CONTENT if deleted, or 404 NOT FOUND
     */
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID uuid) {
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
    @GetMapping("/state/{state}")
    public ResponseEntity<Set<Event>> getEventsByState(@PathVariable EventState state) {
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
    @PatchMapping("/{uuid}")
    public ResponseEntity<Event> patchEvent(
            @PathVariable UUID uuid,
            @RequestBody List<Map<String, Object>> updates) {
        try {
            // 1. Obter o evento da base de datos
            Event existingEvent = eventService.getEventById(uuid);
            if (existingEvent == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 2. Aplicar as operacións JSON-Patch
            Event patchedEvent = patchUtils.applyPatch(existingEvent, updates);

            // 3. Gardar o recurso actualizado
            Event updated = eventService.updateEvent(uuid, patchedEvent);
            return new ResponseEntity<>(updated, HttpStatus.OK);

        } catch (JsonPatchException e) {
            // Erro ao aplicar o parche (operación inválida, path incorrecto, etc.)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
