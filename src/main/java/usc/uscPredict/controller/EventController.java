package usc.uscPredict.controller;

import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.model.Event;
import usc.uscPredict.model.EventState;
import usc.uscPredict.service.EventService;

import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for Event endpoints.
 * Handles HTTP requests for event management.
 * Base path: /events
 */
@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
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
     * PATCH /events/{uuid}/state
     * Changes the state of an event.
     * Body: { "state": "CLOSED" }
     * @param uuid The event UUID
     * @param stateData Object containing the new state
     * @return 200 OK with updated event, or 404 NOT FOUND
     */
    @PatchMapping("/{uuid}/state")
    public ResponseEntity<Event> changeEventState(
            @PathVariable UUID uuid,
            @RequestBody StateChangeRequest stateData) {
        Event updated = eventService.changeEventState(uuid, stateData.getState());
        if (updated != null) {
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Inner class for state change requests.
     * Used for PATCH /events/{uuid}/state endpoint.
     */
    public static class StateChangeRequest {
        private EventState state;

        public EventState getState() {
            return state;
        }

        public void setState(EventState state) {
            this.state = state;
        }
    }
}
