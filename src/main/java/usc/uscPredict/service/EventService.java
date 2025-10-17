package usc.uscPredict.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usc.uscPredict.model.Event;
import usc.uscPredict.model.EventState;
import usc.uscPredict.repository.EventRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service layer for Event entity.
 * Handles business logic for event management.
 */
@Getter
@Service
public class EventService {

    private final EventRepository eventRepository;

    @Autowired
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Retrieves all events in the system.
     * @return Set of all events
     */
    public Set<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    /**
     * Retrieves a single event by its UUID.
     * @param uuid The event's unique identifier
     * @return The event if found, null otherwise
     */
    public Event getEventById(UUID uuid) {
        Optional<Event> event = eventRepository.findById(uuid);
        return event.orElse(null);
    }

    /**
     * Creates a new event.
     * TODO: Add business validations (e.g., title uniqueness, description length)
     * @param event The event to create
     * @return The created event with generated UUID
     */
    @Transactional
    public Event createEvent(Event event) {
        // TODO: Implement business logic here
        // - Validate event data
        // - Check for duplicate titles (optional)
        // - Set default state if not provided
        return eventRepository.save(event);
    }

    /**
     * Updates an existing event.
     * TODO: Add validation logic (e.g., can't modify settled events)
     * @param uuid The UUID of the event to update
     * @param eventData The new event data
     * @return The updated event, or null if not found
     */
    @Transactional
    public Event updateEvent(UUID uuid, Event eventData) {
        Optional<Event> existingOpt = eventRepository.findById(uuid);
        if (existingOpt.isEmpty()) {
            return null;
        }

        Event existing = existingOpt.get();

        // TODO: Implement update logic
        // - Validate state transitions (OPEN -> CLOSED -> SETTLED)
        // - Update only allowed fields
        existing.setTitle(eventData.getTitle());
        existing.setDescription(eventData.getDescription());
        existing.setState(eventData.getState());

        return eventRepository.save(existing);
    }

    /**
     * Deletes an event by UUID.
     * TODO: Add safety checks (can't delete events with active markets)
     * @param uuid The UUID of the event to delete
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean deleteEvent(UUID uuid) {
        if (eventRepository.existsById(uuid)) {
            // TODO: Check if event has associated markets before deleting
            eventRepository.deleteById(uuid);
            return true;
        }
        return false;
    }

    /**
     * Retrieves all events with a specific state.
     * @param state The event state to filter by
     * @return Set of events with the given state
     */
    public Set<Event> getEventsByState(EventState state) {
        return eventRepository.findByState(state);
    }

    /**
     * Changes the state of an event.
     * TODO: Implement state transition validation logic
     * @param uuid The event UUID
     * @param newState The new state
     * @return The updated event, or null if not found
     */
    @Transactional
    public Event changeEventState(UUID uuid, EventState newState) {
        Optional<Event> eventOpt = eventRepository.findById(uuid);
        if (eventOpt.isEmpty()) {
            return null;
        }

        Event event = eventOpt.get();

        // TODO: Validate state transition
        // OPEN -> CLOSED (valid)
        // CLOSED -> SETTLED (valid)
        // SETTLED -> OPEN (invalid)
        event.setState(newState);

        return eventRepository.save(event);
    }
}
