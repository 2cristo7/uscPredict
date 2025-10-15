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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.dto.CreateOrderRequest;
import usc.uscPredict.exception.EventNotFoundException;
import usc.uscPredict.exception.OrderNotFoundException;
import usc.uscPredict.model.Order;
import usc.uscPredict.model.OrderState;
import usc.uscPredict.model.OrderType;
import usc.uscPredict.service.OrderService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Orders", description = "API de gestión de órdenes de compra/venta en eventos")
@RestController
@RequestMapping("/events/{eventId}/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(
            summary = "Listar órdenes de un evento",
            description = "Obtiene todas las órdenes de un evento con filtros dinámicos opcionales por estado, tipo y usuario. " +
                    "Usa Spring Data JPA Specifications para filtrado avanzado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de órdenes obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrdersForEvent(
            @Parameter(description = "UUID del evento", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID eventId,
            @Parameter(description = "Filtrar por estado de la orden", example = "PENDING")
            @RequestParam(required = false) OrderState state,
            @Parameter(description = "Filtrar por tipo de orden (BUY/SELL)", example = "BUY")
            @RequestParam(required = false) OrderType type,
            @Parameter(description = "Filtrar por UUID del usuario", example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam(required = false) UUID userId
    ) {
        try {
            List<Order> orders = orderService.getAllOrdersForEvent(eventId, state, type, userId);
            return new ResponseEntity<>(orders, HttpStatus.OK);
        } catch (EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Operation(
            summary = "Obtener orden específica",
            description = "Recupera los detalles de una orden específica de un evento"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden encontrada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "404", description = "Orden o evento no encontrado", content = @Content)
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(
            @Parameter(description = "UUID del evento", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID eventId,
            @Parameter(description = "UUID de la orden", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID orderId
    ) {
        try {
            Order order = orderService.getOrderById(eventId, orderId);
            return new ResponseEntity<>(order, HttpStatus.OK);
        } catch (OrderNotFoundException | EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Operation(
            summary = "Crear nueva orden",
            description = "Crea una nueva orden en un evento. El estado se establece automáticamente en PENDING. " +
                    "La orden se agrega al order book correspondiente del evento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Orden creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (precio fuera de rango, cantidad negativa, etc.)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Evento o usuario no encontrado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Order> createOrder(
            @Parameter(description = "UUID del evento", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID eventId,
            @Parameter(description = "Datos de la orden a crear", required = true)
            @Valid @RequestBody @NonNull CreateOrderRequest request
    ) {
        try {
            Order createdOrder = orderService.createOrder(eventId, request);
            return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
        } catch (EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(
            summary = "Actualizar orden parcialmente (JSON Patch)",
            description = "Actualiza campos específicos de una orden. " +
                    "Campos permitidos: state (PENDING/EXECUTED/CANCELLED), price, quantity. " +
                    "Content-Type: application/json"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "404", description = "Orden o evento no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o campo desconocido", content = @Content)
    })
    @PatchMapping("/{orderId}")
    public ResponseEntity<Order> partialUpdateOrder(
            @Parameter(description = "UUID del evento", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID eventId,
            @Parameter(description = "UUID de la orden", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID orderId,
            @Parameter(description = "Mapa de campos a actualizar (state, price, quantity)", required = true)
            @RequestBody @NonNull Map<String, Object> updates
    ) {
        try {
            Order updatedOrder = orderService.partialUpdateOrder(eventId, orderId, updates);
            return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
        } catch (OrderNotFoundException | EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(
            summary = "Cancelar orden",
            description = "Cancela una orden cambiando su estado a CANCELLED (soft delete). " +
                    "La orden permanece en la base de datos pero marcada como cancelada."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Orden cancelada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Orden o evento no encontrado", content = @Content)
    })
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "UUID del evento", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID eventId,
            @Parameter(description = "UUID de la orden a cancelar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID orderId
    ) {
        try {
            orderService.deleteOrder(eventId, orderId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (OrderNotFoundException | EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // === Endpoints adicionales ===

    @Operation(
            summary = "Ejecutar orden",
            description = "Marca una orden como ejecutada (estado EXECUTED). " +
                    "Solo se pueden ejecutar órdenes en estado PENDING."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden ejecutada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "404", description = "Orden o evento no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "La orden ya está ejecutada o está cancelada", content = @Content)
    })
    @PostMapping("/{orderId}/execute")
    public ResponseEntity<Order> executeOrder(
            @Parameter(description = "UUID del evento", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID eventId,
            @Parameter(description = "UUID de la orden a ejecutar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID orderId
    ) {
        try {
            Order executedOrder = orderService.executeOrder(eventId, orderId);
            return new ResponseEntity<>(executedOrder, HttpStatus.OK);
        } catch (OrderNotFoundException | EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    @Operation(
            summary = "Obtener estadísticas de órdenes",
            description = "Retorna estadísticas agregadas de las órdenes de un evento: " +
                    "total, pendientes, ejecutadas, canceladas, compras, ventas"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content)
    })
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOrderStats(
            @Parameter(description = "UUID del evento", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID eventId
    ) {
        try {
            Map<String, Object> stats = orderService.getOrderStats(eventId);
            return new ResponseEntity<>(stats, HttpStatus.OK);
        } catch (EventNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
