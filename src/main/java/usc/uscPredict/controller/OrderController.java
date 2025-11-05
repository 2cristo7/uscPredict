package usc.uscPredict.controller;

import com.github.fge.jsonpatch.JsonPatchException;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * Base path: /orders
 */
@RestController
@RequestMapping("/orders")
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
    @GetMapping
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
    @GetMapping("/{uuid}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID uuid) {
        Order order = orderService.getOrderById(uuid);
        if (order != null) {
            return new ResponseEntity<>(order, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * POST /orders
     * Creates a new order (places an order in the market).
     * This triggers the order placement logic including fund locking.
     * @param order The order data (JSON body)
     * @return 201 CREATED with created order, or 400 BAD REQUEST
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody @Valid @NonNull Order order) {
        try {
            Order created = orderService.createOrder(order);
            if (created != null) {
                return new ResponseEntity<>(created, HttpStatus.CREATED);
            } else {
                return ResponseEntity.badRequest().body("User or Market not found");
            }
        } catch (IllegalStateException e) {
            // Handle business logic errors (insufficient funds, invalid market, etc.)
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PUT /orders/{uuid}
     * Updates an existing order.
     * Note: In real systems, modifying orders is often restricted.
     * @param uuid The order UUID
     * @param order The updated order data
     * @return 200 OK with updated order, or 404 NOT FOUND
     */
    @PutMapping("/{uuid}")
    public ResponseEntity<Order> updateOrder(
            @PathVariable UUID uuid,
            @RequestBody @Valid @NonNull Order order) {
        Order updated = orderService.updateOrder(uuid, order);
        if (updated != null) {
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
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
    @PatchMapping("/{uuid}")
    public ResponseEntity<?> patchOrder(
            @PathVariable UUID uuid,
            @RequestBody List<Map<String, Object>> updates) {
        try {
            // 1. Obter a orden da base de datos
            Order existingOrder = orderService.getOrderById(uuid);
            if (existingOrder == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 2. Validar que a orden pode ser modificada (só PENDING ou PARTIALLY_FILLED)
            if (existingOrder.getState() != OrderState.PENDING &&
                existingOrder.getState() != OrderState.PARTIALLY_FILLED) {
                return ResponseEntity.badRequest()
                    .body("Only PENDING or PARTIALLY_FILLED orders can be modified");
            }

            // 3. Aplicar as operacións JSON-Patch
            Order patchedOrder = patchUtils.applyPatch(existingOrder, updates);

            // 4. Gardar a orden actualizada
            Order updated = orderService.patchOrder(uuid, patchedOrder);
            return new ResponseEntity<>(updated, HttpStatus.OK);

        } catch (JsonPatchException e) {
            // Erro ao aplicar o parche (operación inválida, path incorrecto, etc.)
            return ResponseEntity.badRequest().body("Invalid patch operation: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST /orders/{uuid}/cancel
     * Cancels an order and refunds locked funds.
     * @param uuid The order UUID
     * @return 200 OK with cancelled order, or 404 NOT FOUND
     */
    @PostMapping("/{uuid}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable UUID uuid) {
        Order cancelled = orderService.cancelOrder(uuid);
        if (cancelled != null) {
            return new ResponseEntity<>(cancelled, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * DELETE /orders/{uuid}
     * Deletes an order.
     * Note: Consider using cancel instead for audit trail.
     * @param uuid The order UUID
     * @return 204 NO CONTENT if deleted, or 404 NOT FOUND
     */
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID uuid) {
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
    @GetMapping("/user/{userId}")
    public ResponseEntity<Set<Order>> getOrdersByUserId(@PathVariable UUID userId) {
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
    @GetMapping("/market/{marketId}")
    public ResponseEntity<Set<Order>> getOrdersByMarketId(@PathVariable UUID marketId) {
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
    @GetMapping("/market/{marketId}/book")
    public ResponseEntity<Set<Order>> getOrderBook(@PathVariable UUID marketId) {
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
    @GetMapping("/market/{marketId}/state/{state}")
    public ResponseEntity<Set<Order>> getOrdersByMarketIdAndState(
            @PathVariable UUID marketId,
            @PathVariable OrderState state) {
        Set<Order> orders = orderService.getOrdersByMarketIdAndState(marketId, state);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }
}
