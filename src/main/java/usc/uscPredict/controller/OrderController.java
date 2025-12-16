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
import usc.uscPredict.model.Order;
import usc.uscPredict.model.OrderState;
import usc.uscPredict.service.OrderService;
import usc.uscPredict.util.PatchUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for Order endpoints.
 * Handles HTTP requests for order management and trading.
 * Base path: /api/v1/orders
 */
@Tag(name = "Orders", description = "API de gestión de órdenes de trading")
@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {

    private final OrderService orderService;
    private final PatchUtils patchUtils;

    @Autowired
    public OrderController(OrderService orderService, PatchUtils patchUtils) {
        this.orderService = orderService;
        this.patchUtils = patchUtils;
    }

    /**
     * GET /orders
     * Retrieves all orders.
     * @return 200 OK with list of orders
     */
    @Operation(
            summary = "Listar todas las órdenes",
            description = "Obtiene una lista completa de todas las órdenes de trading en la plataforma"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de órdenes obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class)))
    })
    @GetMapping
    @JsonView(Order.OrderSummaryView.class)
    public ResponseEntity<Set<Order>> getAllOrders() {
        Set<Order> orders = orderService.getAllOrders();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    /**
     * GET /orders/{uuid}
     * Retrieves a single order by UUID.
     * @param uuid The order UUID
     * @return 200 OK with order, or 404 NOT FOUND
     */
    @Operation(
            summary = "Obtener orden por ID",
            description = "Busca y retorna una orden específica utilizando su identificador UUID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden encontrada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content)
    })
    @GetMapping("/{uuid}")
    @JsonView(Order.OrderDetailView.class)
    public ResponseEntity<Order> getOrderById(
            @Parameter(description = "UUID de la orden a buscar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
        Order order = orderService.getOrderById(uuid);
        return ResponseEntity.ok(order);
    }

    /**
     * POST /orders
     * Creates a new order (places an order in the market).
     * This triggers the order placement logic including fund locking.
     * @param order The order data (JSON body)
     * @return 201 CREATED with created order, or 400 BAD REQUEST
     */
    @Operation(
            summary = "Crear nueva orden",
            description = "Coloca una nueva orden de compra/venta en el mercado. Bloquea los fondos necesarios en la cartera del usuario automáticamente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Orden creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o fondos insuficientes", content = @Content)
    })
    @PostMapping
    @JsonView(Order.OrderDetailView.class)
    public ResponseEntity<Order> createOrder(@RequestBody @Valid @NonNull Order order) {
        Order created = orderService.createOrder(order);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * PUT /orders/{uuid}
     * Updates an existing order.
     * Note: In real systems, modifying orders is often restricted.
     * @param uuid The order UUID
     * @param order The updated order data
     * @return 200 OK with updated order, or 404 NOT FOUND
     */
    @Operation(
            summary = "Actualizar orden completa",
            description = "Actualiza todos los campos de una orden existente. Nota: En sistemas reales, la modificación de órdenes suele estar restringida"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content)
    })
    @PutMapping("/{uuid}")
    @JsonView(Order.OrderDetailView.class)
    public ResponseEntity<Order> updateOrder(
            @Parameter(description = "UUID de la orden a actualizar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid,
            @RequestBody @Valid @NonNull Order order) {
        Order updated = orderService.updateOrder(uuid, order);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /orders/{uuid}
     * Applies JSON-Patch operations to modify an order (RFC 6902).
     * Only PENDING or PARTIALLY_FILLED orders can be modified.
     * Allows updating price and quantity fields.
     * Body: [{ "op": "replace", "path": "/price", "value": 0.75 }]
     * @param uuid The order UUID
     * @param updates List of JSON-Patch operations
     * @return 200 OK with updated order, 404 NOT FOUND, or 400 BAD REQUEST
     */
    @Operation(
            summary = "Actualizar orden parcialmente con JSON-Patch",
            description = "Aplica operaciones JSON-Patch (RFC 6902) para modificar campos específicos. Solo se pueden modificar órdenes en estado PENDING o PARTIALLY_FILLED"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content),
            @ApiResponse(responseCode = "400", description = "Operación JSON-Patch inválida o estado de orden no modificable", content = @Content)
    })
    @PatchMapping("/{uuid}")
    @JsonView(Order.OrderDetailView.class)
    public ResponseEntity<Order> patchOrder(
            @Parameter(description = "UUID de la orden a actualizar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid,
            @RequestBody List<Map<String, Object>> updates) throws JsonPatchException {
        // 1. Obter a orden da base de datos (throws exception if not found)
        Order existingOrder = orderService.getOrderById(uuid);

        // 2. Validar que a orden pode ser modificada (só PENDING ou PARTIALLY_FILLED)
        if (existingOrder.getState() != OrderState.PENDING &&
            existingOrder.getState() != OrderState.PARTIALLY_FILLED) {
            throw new IllegalStateException("Only PENDING or PARTIALLY_FILLED orders can be modified");
        }

        // 3. Aplicar as operacións JSON-Patch (JsonPatchException handled globally)
        Order patchedOrder = patchUtils.applyPatch(existingOrder, updates);

        // 4. Gardar a orden actualizada (IllegalStateException handled globally)
        Order updated = orderService.patchOrder(uuid, patchedOrder);
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /orders/{uuid}/cancel
     * Cancels an order and refunds locked funds.
     * @param uuid The order UUID
     * @return 200 OK with cancelled order, or 404 NOT FOUND
     */
    @Operation(
            summary = "Cancelar orden",
            description = "Cancela una orden pendiente y devuelve los fondos bloqueados a la cartera del usuario"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden cancelada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content),
            @ApiResponse(responseCode = "400", description = "La orden no puede ser cancelada en su estado actual", content = @Content)
    })
    @PostMapping("/{uuid}/cancel")
    @JsonView(Order.OrderDetailView.class)
    public ResponseEntity<Order> cancelOrder(
            @Parameter(description = "UUID de la orden a cancelar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
        Order cancelled = orderService.cancelOrder(uuid);
        return ResponseEntity.ok(cancelled);
    }

    /**
     * DELETE /orders/{uuid}
     * Deletes an order.
     * Note: Consider using cancel instead for audit trail.
     * @param uuid The order UUID
     * @return 204 NO CONTENT if deleted, or 404 NOT FOUND
     */
    @Operation(
            summary = "Eliminar orden",
            description = "Elimina permanentemente una orden del sistema. Nota: Considere usar cancelar en su lugar para mantener registro de auditoría"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Orden eliminada exitosamente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content)
    })
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "UUID de la orden a eliminar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
        boolean deleted = orderService.deleteOrder(uuid);
        if (deleted) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * GET /orders/user/{userId}
     * Retrieves all orders for a specific user.
     * Example: GET /orders/user/123e4567-e89b-12d3-a456-426614174000
     * @param userId The user UUID
     * @return 200 OK with list of orders
     */
    @Operation(
            summary = "Buscar órdenes por usuario",
            description = "Retorna todas las órdenes realizadas por un usuario específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de órdenes obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class)))
    })
    @GetMapping("/user/{userId}")
    @JsonView(Order.OrderSummaryView.class)
    public ResponseEntity<Set<Order>> getOrdersByUserId(
            @Parameter(description = "UUID del usuario", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        Set<Order> orders = orderService.getOrdersByUserId(userId);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    /**
     * GET /orders/market/{marketId}
     * Retrieves all orders for a specific market.
     * Example: GET /orders/market/123e4567-e89b-12d3-a456-426614174000
     * @param marketId The market UUID
     * @return 200 OK with list of orders
     */
    @Operation(
            summary = "Buscar órdenes por mercado",
            description = "Retorna todas las órdenes colocadas en un mercado específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de órdenes obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class)))
    })
    @GetMapping("/market/{marketId}")
    @JsonView(Order.OrderSummaryView.class)
    public ResponseEntity<Set<Order>> getOrdersByMarketId(
            @Parameter(description = "UUID del mercado", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID marketId) {
        Set<Order> orders = orderService.getOrdersByMarketId(marketId);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    /**
     * GET /orders/market/{marketId}/book
     * Retrieves the order book for a market (all pending orders).
     * This shows all active buy and sell orders.
     * Example: GET /orders/market/123e4567-e89b-12d3-a456-426614174000/book
     * @param marketId The market UUID
     * @return 200 OK with order book
     */
    @Operation(
            summary = "Obtener libro de órdenes",
            description = "Retorna el libro de órdenes completo de un mercado (todas las órdenes activas de compra y venta)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Libro de órdenes obtenido exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class)))
    })
    @GetMapping("/market/{marketId}/book")
    @JsonView(Order.OrderSummaryView.class)
    public ResponseEntity<Set<Order>> getOrderBook(
            @Parameter(description = "UUID del mercado", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID marketId) {
        Set<Order> orderBook = orderService.getOrderBook(marketId);
        return new ResponseEntity<>(orderBook, HttpStatus.OK);
    }

    /**
     * GET /orders/market/{marketId}/state/{state}
     * Retrieves orders for a market filtered by state.
     * Example: GET /orders/market/123e4567.../state/FILLED
     * @param marketId The market UUID
     * @param state The order state
     * @return 200 OK with list of orders
     */
    @Operation(
            summary = "Buscar órdenes por mercado y estado",
            description = "Retorna todas las órdenes de un mercado filtradas por su estado específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de órdenes obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class)))
    })
    @GetMapping("/market/{marketId}/state/{state}")
    @JsonView(Order.OrderSummaryView.class)
    public ResponseEntity<Set<Order>> getOrdersByMarketIdAndState(
            @Parameter(description = "UUID del mercado", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID marketId,
            @Parameter(description = "Estado de la orden", required = true, example = "FILLED")
            @PathVariable OrderState state) {
        Set<Order> orders = orderService.getOrdersByMarketIdAndState(marketId, state);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }
}
