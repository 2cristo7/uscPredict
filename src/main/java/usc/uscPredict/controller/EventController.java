package usc.uscPredict.controller;

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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.dto.AddOrderRequest;
import usc.uscPredict.exception.EventNotFoundException;
import usc.uscPredict.model.Event;
import usc.uscPredict.model.EventState;
import usc.uscPredict.service.EventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Events", description = "API de gestión de eventos de predicción")
@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // === CRUD Endpoints ===

    @Operation(
            summary = "Listar todos los eventos",
            description = "Obtiene una lista de todos los eventos de predicción con filtros opcionales por estado, creador y rango de fechas"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class)))
    })
    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents(
            @Parameter(description = "Filtrar por estado del evento (ACTIVE, CLOSED, RESOLVED)", example = "ACTIVE")
            @RequestParam(required = false) EventState state,
            @Parameter(description = "Filtrar por UUID del creador del evento", example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam(required = false) UUID creatorUuid,
            @Parameter(description = "Fecha de inicio del rango de búsqueda (formato ISO 8601)", example = "2025-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Fecha de fin del rango de búsqueda (formato ISO 8601)", example = "2025-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        List<Event> events;

        if (state != null || creatorUuid != null || (startDate != null && endDate != null)) {
            events = eventService.getAllEvents(state, creatorUuid, startDate, endDate);
        } else {
            events = eventService.getAllEvents();
        }

        return new ResponseEntity<>(events, HttpStatus.OK);
    }

    @Operation(
            summary = "Obtener evento por ID",
            description = "Recupera los detalles completos de un evento específico usando su identificador único"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento encontrado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(
            @Parameter(description = "UUID del evento a buscar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        try {
            Event event = eventService.getEventById(id);
            return new ResponseEntity<>(event, HttpStatus.OK);
        } catch (EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Operation(
            summary = "Crear nuevo evento",
            description = "Crea un nuevo evento de predicción. Requiere título, descripción, fechas, precios y creador. " +
                    "El estado se establece en ACTIVE por defecto."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Evento creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej: fecha de fin anterior a fecha de inicio)", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Event> createEvent(
            @Parameter(description = "Datos del evento a crear", required = true)
            @Valid @RequestBody @NonNull Event event) {
        try {
            Event createdEvent = eventService.createEvent(event);
            return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(
            summary = "Actualizar evento completamente",
            description = "Actualiza todos los campos de un evento existente (excepto ID, creador y order books)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(
            @Parameter(description = "UUID del evento a actualizar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @Parameter(description = "Datos actualizados del evento", required = true)
            @Valid @RequestBody @NonNull Event event
    ) {
        try {
            Event updatedEvent = eventService.updateEvent(id, event);
            return new ResponseEntity<>(updatedEvent, HttpStatus.OK);
        } catch (EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(
            summary = "Actualizar evento parcialmente",
            description = "Actualiza solo los campos especificados del evento (estilo JSON Patch). " +
                    "Campos permitidos: title, description, startDate, endDate, state, yesPrice, noPrice"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o campo desconocido", content = @Content)
    })
    @PatchMapping("/{id}")
    public ResponseEntity<Event> partialUpdateEvent(
            @Parameter(description = "UUID del evento a actualizar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @Parameter(description = "Mapa de campos a actualizar (clave: nombre del campo, valor: nuevo valor)", required = true)
            @RequestBody @NonNull Map<String, Object> updates
    ) {
        try {
            Event updatedEvent = eventService.partialUpdateEvent(id, updates);
            return new ResponseEntity<>(updatedEvent, HttpStatus.OK);
        } catch (EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(
            summary = "Eliminar evento (soft delete)",
            description = "Desactiva un evento cambiando su estado a CLOSED. No elimina el evento de la base de datos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Evento desactivado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "UUID del evento a eliminar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        try {
            eventService.deleteEvent(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // === Nested Order Operations ===

    @Operation(
            summary = "Agregar orden a un evento",
            description = "Agrega una orden (compra/venta) al order book YES o NO del evento",
            tags = "Orders"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden agregada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Tipo de orden inválido (debe ser 'yes' o 'no')", content = @Content)
    })
    @PostMapping("/{id}/orders")
    public ResponseEntity<Event> addOrderToEvent(
            @Parameter(description = "UUID del evento", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @Parameter(description = "Request con UUID de la orden y tipo (yes/no)", required = true)
            @Valid @RequestBody @NonNull AddOrderRequest request
    ) {
        try {
            Event updatedEvent = eventService.addOrderToEvent(id, request.getOrderId(), request.getType());
            return new ResponseEntity<>(updatedEvent, HttpStatus.OK);
        } catch (EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(
            summary = "Listar órdenes de un evento",
            description = "Obtiene la lista de UUIDs de órdenes para un evento. Puede filtrar por tipo (yes/no) o mostrar todas",
            tags = "Orders"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de órdenes obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Tipo de orden inválido", content = @Content)
    })
    @GetMapping("/{id}/orders")
    public ResponseEntity<List<UUID>> getOrdersForEvent(
            @Parameter(description = "UUID del evento", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @Parameter(description = "Filtrar por tipo de orden: 'yes' o 'no'. Si se omite, retorna todas las órdenes", example = "yes")
            @RequestParam(required = false) String type
    ) {
        try {
            List<UUID> orders = eventService.getOrdersForEvent(id, type);
            return new ResponseEntity<>(orders, HttpStatus.OK);
        } catch (EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // === Special Operations ===

    @Operation(
            summary = "Resolver un evento",
            description = "Marca un evento como RESOLVED. Esta acción es irreversible y típicamente se realiza cuando el resultado es conocido."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento resuelto exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "El evento ya está resuelto", content = @Content)
    })
    @PostMapping("/{id}/resolve")
    public ResponseEntity<Event> resolveEvent(
            @Parameter(description = "UUID del evento a resolver", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        try {
            Event resolvedEvent = eventService.resolveEvent(id);
            return new ResponseEntity<>(resolvedEvent, HttpStatus.OK);
        } catch (EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    @Operation(
            summary = "Obtener estadísticas del evento",
            description = "Retorna métricas agregadas del evento: precios, cantidad de órdenes, estado, si está en progreso, etc."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content)
    })
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getEventStats(
            @Parameter(description = "UUID del evento", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        try {
            Map<String, Object> stats = eventService.getEventStats(id);
            return new ResponseEntity<>(stats, HttpStatus.OK);
        } catch (EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // === Utility Endpoints ===

    @Operation(
            summary = "Obtener eventos activos en progreso",
            description = "Retorna eventos con estado ACTIVE que ya comenzaron pero aún no terminaron"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos activos obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class)))
    })
    @GetMapping("/active")
    public ResponseEntity<List<Event>> getActiveEventsInProgress() {
        List<Event> activeEvents = eventService.getActiveEventsInProgress();
        return new ResponseEntity<>(activeEvents, HttpStatus.OK);
    }

    @Operation(
            summary = "Cerrar eventos expirados",
            description = "Cierra automáticamente todos los eventos ACTIVE cuya fecha de fin ya pasó, cambiándolos a CLOSED"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Eventos cerrados exitosamente. Retorna lista de eventos cerrados",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Event.class)))
    })
    @PostMapping("/close-expired")
    public ResponseEntity<List<Event>> closeExpiredEvents() {
        List<Event> closedEvents = eventService.closeExpiredEvents();
        return new ResponseEntity<>(closedEvents, HttpStatus.OK);
    }

    @Operation(
            summary = "Health check",
            description = "Verifica que el servicio de eventos esté funcionando correctamente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicio funcionando correctamente")
    })
    @GetMapping("/health")
    public Map<String, Object> getHealthStatus() {
        return Map.of(
                "status", "OK",
                "service", "Events API",
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
