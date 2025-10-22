package usc.uscPredict.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usc.uscPredict.model.*;
import usc.uscPredict.repository.MarketRepository;
import usc.uscPredict.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service layer for Order entity.
 * Orchestrates order creation, cancellation, and retrieval operations.
 *
 * ARCHITECTURE DECISION: Separation of Concerns
 * ==============================================
 * This service acts as an ORCHESTRATOR, coordinating between:
 * - OrderPersistenceService: Handles transactional order persistence
 * - MarketService: Handles order matching logic
 *
 * WHY NOT HANDLE PERSISTENCE HERE?
 * --------------------------------
 * Order persistence is delegated to OrderPersistenceService to enable proper
 * Spring @Transactional behavior. See OrderPersistenceService documentation
 * for detailed explanation of the self-invocation problem.
 *
 * TRANSACTIONAL DESIGN:
 * --------------------
 * Order creation has TWO separate transactional phases:
 *
 * Phase 1 (OrderPersistenceService): Persist order
 *   - Validate user/market
 *   - Lock funds
 *   - Save order as PENDING
 *   - Create transaction record
 *   → If fails: Complete rollback, order NOT created
 *
 * Phase 2 (MarketService): Attempt matching
 *   - Match against existing orders
 *   - Update positions if matched
 *   → If fails: Order remains PENDING, can be matched later
 *
 * This separation ensures that valid orders are never lost due to matching failures.
 *
 * @see OrderPersistenceService - Handles transactional order persistence
 * @see MarketService#matchOrders(UUID) - Handles order matching in separate transaction
 */
@Getter
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MarketRepository marketRepository;
    private final WalletService walletService;
    private final MarketService marketService;
    private final OrderPersistenceService orderPersistenceService;

    @Autowired
    public OrderService(
            OrderRepository orderRepository,
            MarketRepository marketRepository,
            WalletService walletService,
            MarketService marketService,
            OrderPersistenceService orderPersistenceService) {
        this.orderRepository = orderRepository;
        this.marketRepository = marketRepository;
        this.walletService = walletService;
        this.marketService = marketService;
        this.orderPersistenceService = orderPersistenceService;
    }

    /**
     * Retrieves all orders in the system.
     * @return Set of all orders
     */
    public Set<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Retrieves a single order by its UUID.
     * @param uuid The order's unique identifier
     * @return The order if found, null otherwise
     */
    public Order getOrderById(UUID uuid) {
        Optional<Order> order = orderRepository.findById(uuid);
        return order.orElse(null);
    }

    /**
     * Creates a new order (places an order).
     *
     * DESIGN PATTERN: Two-Phase Orchestration
     * ========================================
     * This method orchestrates order creation in TWO independent transactional phases:
     *
     * PHASE 1: Order Persistence (Transactional)
     * ------------------------------------------
     * Delegates to OrderPersistenceService.persistOrder()
     * - Validates user and market
     * - Calculates and locks required funds
     * - Saves order as PENDING
     * - Creates transaction audit record
     *
     * Transaction boundary: Managed by OrderPersistenceService
     * On failure: Complete rollback, order NOT created
     * On success: Order exists in DB as PENDING, funds are locked
     *
     * PHASE 2: Order Matching (Separate Transaction)
     * ----------------------------------------------
     * Attempts to match the new order against existing orders via MarketService
     * - Executed in a SEPARATE transaction (managed by MarketService)
     * - Failures are caught and logged, but do NOT rollback Phase 1
     *
     * Transaction boundary: Managed by MarketService.matchOrders()
     * On failure: Order remains PENDING, can be matched later manually
     * On success: Order state updated to FILLED/PARTIALLY_FILLED
     *
     * WHY TWO PHASES?
     * ---------------
     * If matching were in the same transaction as persistence:
     * - Matching failure → Order creation rollback → Valid order LOST ❌
     *
     * With two phases:
     * - Matching failure → Order remains PENDING → Can retry matching ✅
     *
     * IMPORTANT: Cross-Service Call
     * -----------------------------
     * This method calls OrderPersistenceService (different class), which ensures
     * the @Transactional annotation works correctly via Spring's AOP proxy.
     * DO NOT move persistence logic back into this class - it will break transactions!
     *
     * @param order The order to create (uuid will be auto-generated)
     * @return The created order with generated UUID
     * @throws IllegalArgumentException if user or market not found
     * @throws IllegalStateException if insufficient wallet balance
     *
     * @see OrderPersistenceService#persistOrder(Order) - Phase 1 implementation
     * @see MarketService#matchOrders(UUID) - Phase 2 implementation
     */
    public Order createOrder(Order order) {
        // ═══════════════════════════════════════════════════════════════════
        // PHASE 1: Persist Order (Atomic Transaction)
        // ═══════════════════════════════════════════════════════════════════
        // This call crosses service boundaries, ensuring @Transactional works
        Order createdOrder = orderPersistenceService.persistOrder(order);

        // ═══════════════════════════════════════════════════════════════════
        // PHASE 2: Attempt Order Matching (Separate Transaction)
        // ═══════════════════════════════════════════════════════════════════
        // Matching failures are isolated from order creation
        try {
            Market market = marketRepository.findById(order.getMarketId()).orElse(null);
            if (market != null && market.getStatus() == MarketStatus.ACTIVE) {
                // MarketService.matchOrders() runs in its own @Transactional boundary
                marketService.matchOrders(order.getMarketId());
            }
        } catch (Exception e) {
            // Log the matching error but DO NOT propagate the exception
            // The order is already safely persisted in the database
            // Developers can manually trigger matching via:
            // - MarketController.matchOrders() endpoint
            // - Frontend "Manual Order Matching" button
            System.err.println(
                    "WARNING: Order matching failed for order " + createdOrder.getUuid() +
                    ". Order remains PENDING and can be matched manually. " +
                    "Error: " + e.getMessage()
            );
        }

        return createdOrder;
    }

    /**
     * Updates an existing order.
     * TODO: Add validation (only allow updating PENDING orders)
     * @param uuid The UUID of the order to update
     * @param orderData The new order data
     * @return The updated order, or null if not found
     */
    @Transactional
    public Order updateOrder(UUID uuid, Order orderData) {
        Optional<Order> existingOpt = orderRepository.findById(uuid);
        if (existingOpt.isEmpty()) {
            return null;
        }

        Order existing = existingOpt.get();

        // TODO: Validate that order can be updated
        // - Only PENDING orders can be modified
        // - If partially filled, only quantity can increase
        existing.setPrice(orderData.getPrice());
        existing.setQuantity(orderData.getQuantity());
        existing.setState(orderData.getState());

        return orderRepository.save(existing);
    }

    /**
     * Cancels an order.
     * - Unlock funds in wallet
     * - Create ORDER_CANCELLED transaction
     * - Refund any locked amounts
     * @param uuid The UUID of the order to cancel
     * @return The cancelled order, or null if not found
     */
    @Transactional
    public Order cancelOrder(UUID uuid) {
        Optional<Order> orderOpt = orderRepository.findById(uuid);
        if (orderOpt.isEmpty()) {
            return null;
        }

        Order order = orderOpt.get();

        // 1. Check order is PENDING or PARTIALLY_FILLED
        if (order.getState() != OrderState.PENDING &&
                order.getState() != OrderState.PARTIALLY_FILLED) {
            throw new IllegalStateException("Only PENDING or PARTIALLY_FILLED orders can be cancelled");
        }
        // 2. Calculate locked amount for unfilled quantity
        int unfilledQuantity = order.getQuantity()-order.getFilledQuantity();
        BigDecimal lockedAmount = order.getPrice().multiply(BigDecimal.valueOf(unfilledQuantity));

        // 3. Unlock funds in wallet
        walletService.unlockFunds(order.getUserId(), lockedAmount);

        // 4. Create ORDER_CANCELLED transaction
        Transaction transaction = new Transaction(
                order.getUserId(),
                TransactionType.ORDER_CANCELLED,
                lockedAmount
        );

        // 5. Set state to CANCELLED
        order.setState(OrderState.CANCELLED);

        return orderRepository.save(order);
    }

    /**
     * Deletes an order by UUID.
     * Note: In production, orders should be cancelled, not deleted (for audit trail)
     * @param uuid The UUID of the order to delete
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean deleteOrder(UUID uuid) {
        if (orderRepository.existsById(uuid)) {
            // TODO: Consider soft-delete instead of hard delete
            orderRepository.deleteById(uuid);
            return true;
        }
        return false;
    }

    /**
     * Retrieves all orders for a specific user.
     * @param userId The user UUID
     * @return Set of orders belonging to the user
     */
    public Set<Order> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserId(userId);
    }

    /**
     * Retrieves all orders for a specific market.
     * @param marketId The market UUID
     * @return Set of orders for the market
     */
    public Set<Order> getOrdersByMarketId(UUID marketId) {
        return orderRepository.findByMarketId(marketId);
    }

    /**
     * Retrieves all orders for a market with a specific state.
     * Useful for getting the order book (all PENDING orders).
     * @param marketId The market UUID
     * @param state The order state
     * @return Set of orders matching criteria
     */
    public Set<Order> getOrdersByMarketIdAndState(UUID marketId, OrderState state) {
        return orderRepository.findByMarketIdAndState(marketId, state);
    }

    /**
     * Gets the order book for a market (all pending buy and sell orders).
     * @param marketId The market UUID
     * @return Set of pending orders (the order book)
     */
    public Set<Order> getOrderBook(UUID marketId) {
        return orderRepository.findByMarketIdAndState(marketId, OrderState.PENDING);
    }
}
