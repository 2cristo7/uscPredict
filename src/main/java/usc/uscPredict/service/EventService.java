package usc.uscPredict.service;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import usc.uscPredict.exception.EventNotFoundException;
import usc.uscPredict.model.Event;
import usc.uscPredict.model.EventState;
import usc.uscPredict.model.User;
import usc.uscPredict.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class EventService {

    private final EventRepository eventRepository;

    @Autowired
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // === CRUD Operations ===

    /**
     * Obtener todos los eventos
     */
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    /**
     * Obtener todos los eventos con filtros opcionales
     */
    public List<Event> getAllEvents(EventState state, UUID creatorUuid, LocalDateTime startDate, LocalDateTime endDate) {
        // Si hay estado y creador
        if (state != null && creatorUuid != null) {
            return eventRepository.findByStateAndCreatedBy_Uuid(state, creatorUuid);
        }

        // Solo por estado
        if (state != null) {
            return eventRepository.findByState(state);
        }

        // Solo por creador
        if (creatorUuid != null) {
            return eventRepository.findByCreatedBy_Uuid(creatorUuid);
        }

        // Solo por rango de fechas
        if (startDate != null && endDate != null) {
            return eventRepository.findByStartDateBetween(startDate, endDate);
        }

        // Sin filtros
        return eventRepository.findAll();
    }

    /**
     * Obtener evento por ID
     */
    public Event getEventById(@NonNull UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + id));
    }

    /**
     * Crear un nuevo evento
     */
    public Event createEvent(@NonNull Event event) {
        // Validar fechas
        if (event.getEndDate().isBefore(event.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Establecer estado por defecto si no está definido
        if (event.getState() == null) {
            event.setState(EventState.ACTIVE);
        }

        // Inicializar order books si son nulos
        if (event.getYesOrderBook() == null) {
            event.setYesOrderBook(new ArrayList<>());
        }
        if (event.getNoOrderBook() == null) {
            event.setNoOrderBook(new ArrayList<>());
        }

        // Validar precios
        if (event.getYesPrice() == null || event.getNoPrice() == null) {
            throw new IllegalArgumentException("Yes and No prices must be set");
        }

        return eventRepository.save(event);
    }

    /**
     * Actualización completa de un evento
     */
    public Event updateEvent(@NonNull UUID id, @NonNull Event updatedEvent) {
        Event existingEvent = getEventById(id);

        // Validar fechas si se están actualizando
        if (updatedEvent.getEndDate().isBefore(updatedEvent.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Actualizar campos
        existingEvent.setTitle(updatedEvent.getTitle());
        existingEvent.setDescription(updatedEvent.getDescription());
        existingEvent.setStartDate(updatedEvent.getStartDate());
        existingEvent.setEndDate(updatedEvent.getEndDate());
        existingEvent.setState(updatedEvent.getState());
        existingEvent.setYesPrice(updatedEvent.getYesPrice());
        existingEvent.setNoPrice(updatedEvent.getNoPrice());

        // No actualizamos createdBy ni order books en una actualización completa

        return eventRepository.save(existingEvent);
    }

    /**
     * Actualización parcial de un evento (estilo JSON Patch)
     */
    public Event partialUpdateEvent(@NonNull UUID id, @NonNull Map<String, Object> updates) {
        Event event = getEventById(id);

        updates.forEach((key, value) -> {
            switch (key) {
                case "title":
                    event.setTitle((String) value);
                    break;
                case "description":
                    event.setDescription((String) value);
                    break;
                case "startDate":
                    event.setStartDate(LocalDateTime.parse((String) value));
                    break;
                case "endDate":
                    event.setEndDate(LocalDateTime.parse((String) value));
                    break;
                case "state":
                    event.setState(EventState.valueOf((String) value));
                    break;
                case "yesPrice":
                    event.setYesPrice(((Number) value).doubleValue());
                    break;
                case "noPrice":
                    event.setNoPrice(((Number) value).doubleValue());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown field: " + key);
            }
        });

        // Validar fechas después de la actualización
        if (event.getEndDate().isBefore(event.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        return eventRepository.save(event);
    }

    /**
     * Eliminar o desactivar un evento (soft delete)
     */
    public void deleteEvent(@NonNull UUID id) {
        Event event = getEventById(id);
        // Soft delete: cambiar estado a CLOSED
        event.setState(EventState.CLOSED);
        eventRepository.save(event);
    }

    /**
     * Eliminación permanente (solo si es necesario)
     */
    public void hardDeleteEvent(@NonNull UUID id) {
        Event event = getEventById(id);
        eventRepository.delete(event);
    }

    // === Order Book Operations ===

    /**
     * Agregar una orden al order book de un evento
     */
    public Event addOrderToEvent(@NonNull UUID eventId, @NonNull UUID orderId, @NonNull String type) {
        Event event = getEventById(eventId);

        if ("yes".equalsIgnoreCase(type)) {
            if (event.getYesOrderBook() == null) {
                event.setYesOrderBook(new ArrayList<>());
            }
            event.getYesOrderBook().add(orderId);
        } else if ("no".equalsIgnoreCase(type)) {
            if (event.getNoOrderBook() == null) {
                event.setNoOrderBook(new ArrayList<>());
            }
            event.getNoOrderBook().add(orderId);
        } else {
            throw new IllegalArgumentException("Order type must be 'yes' or 'no'");
        }

        return eventRepository.save(event);
    }

    /**
     * Obtener órdenes de un evento
     */
    public List<UUID> getOrdersForEvent(@NonNull UUID eventId, String type) {
        Event event = getEventById(eventId);

        if (type == null || type.isEmpty()) {
            // Retornar todas las órdenes
            List<UUID> allOrders = new ArrayList<>();
            if (event.getYesOrderBook() != null) {
                allOrders.addAll(event.getYesOrderBook());
            }
            if (event.getNoOrderBook() != null) {
                allOrders.addAll(event.getNoOrderBook());
            }
            return allOrders;
        }

        if ("yes".equalsIgnoreCase(type)) {
            return event.getYesOrderBook() != null ? event.getYesOrderBook() : new ArrayList<>();
        } else if ("no".equalsIgnoreCase(type)) {
            return event.getNoOrderBook() != null ? event.getNoOrderBook() : new ArrayList<>();
        } else {
            throw new IllegalArgumentException("Order type must be 'yes' or 'no'");
        }
    }

    // === Special Operations ===

    /**
     * Resolver un evento (cambiar estado a RESOLVED)
     */
    public Event resolveEvent(@NonNull UUID id) {
        Event event = getEventById(id);

        if (event.getState() == EventState.RESOLVED) {
            throw new IllegalStateException("Event is already resolved");
        }

        event.setState(EventState.RESOLVED);
        return eventRepository.save(event);
    }

    /**
     * Obtener estadísticas de un evento
     */
    public Map<String, Object> getEventStats(@NonNull UUID id) {
        Event event = getEventById(id);

        Map<String, Object> stats = new HashMap<>();
        stats.put("eventId", event.getId());
        stats.put("title", event.getTitle());
        stats.put("state", event.getState());
        stats.put("yesPrice", event.getYesPrice());
        stats.put("noPrice", event.getNoPrice());
        stats.put("yesOrderCount", event.getYesOrderBook() != null ? event.getYesOrderBook().size() : 0);
        stats.put("noOrderCount", event.getNoOrderBook() != null ? event.getNoOrderBook().size() : 0);
        stats.put("totalOrders",
            (event.getYesOrderBook() != null ? event.getYesOrderBook().size() : 0) +
            (event.getNoOrderBook() != null ? event.getNoOrderBook().size() : 0)
        );
        stats.put("startDate", event.getStartDate());
        stats.put("endDate", event.getEndDate());
        stats.put("isActive", event.getState() == EventState.ACTIVE);
        stats.put("isResolved", event.getState() == EventState.RESOLVED);

        // Calcular si el evento está en progreso
        LocalDateTime now = LocalDateTime.now();
        boolean inProgress = event.getState() == EventState.ACTIVE &&
                           now.isAfter(event.getStartDate()) &&
                           now.isBefore(event.getEndDate());
        stats.put("inProgress", inProgress);

        return stats;
    }

    /**
     * Cerrar eventos que han pasado su fecha de fin
     */
    public List<Event> closeExpiredEvents() {
        List<Event> expiredEvents = eventRepository.findEventsToClose(LocalDateTime.now());
        expiredEvents.forEach(event -> event.setState(EventState.CLOSED));
        return eventRepository.saveAll(expiredEvents);
    }

    /**
     * Obtener eventos activos en progreso
     */
    public List<Event> getActiveEventsInProgress() {
        return eventRepository.findActiveEventsInProgress(EventState.ACTIVE, LocalDateTime.now());
    }
}
