package usc.uscPredict.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usc.uscPredict.model.Order;
import usc.uscPredict.model.OrderState;
import usc.uscPredict.repository.MarketRepository;
import usc.uscPredict.repository.OrderRepository;
import usc.uscPredict.repository.UserRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service layer for Order entity.
 * Handles business logic for order management and matching.
 */
@Getter
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;

    @Autowired
    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            MarketRepository marketRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.marketRepository = marketRepository;
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
     * TODO: THIS IS WHERE YOUR MAIN LOGIC GOES
     * - Verify user exists and has sufficient wallet balance
     * - Lock funds in wallet
     * - Create transaction record
     * - Attempt to match with existing orders
     * - Update positions if matched
     * @param order The order to create
     * @return The created order with generated UUID
     */
    @Transactional
    public Order createOrder(Order order) {
        // TODO: Implement order placement logic
        // 1. Validate user exists
        // 2. Validate market exists and is ACTIVE
        // 3. Calculate required funds (price * quantity)
        // 4. Check wallet has sufficient balance
        // 5. Lock funds in wallet
        // 6. Save order with state PENDING
        // 7. Create ORDER_PLACED transaction
        // 8. Attempt to match with counter-orders (optional, can be async)

        return orderRepository.save(order);
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
     * TODO: Implement cancellation logic:
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

        // TODO: Implement cancellation logic
        // 1. Check order is PENDING or PARTIALLY_FILLED
        // 2. Calculate locked amount for unfilled quantity
        // 3. Unlock funds in wallet
        // 4. Create ORDER_CANCELLED transaction
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

    /**
     * Matches orders for a specific market.
     * TODO: THIS IS YOUR MATCHING ENGINE - IMPLEMENT YOUR CUSTOM LOGIC HERE
     * Should find compatible BUY and SELL orders and execute trades.
     * @param marketId The market UUID
     * @return Number of matches executed
     */
    @Transactional
    public int matchOrders(UUID marketId) {
        // TODO: Implement order matching logic
        // 1. Get all PENDING BUY orders for market, sorted by price DESC
        // 2. Get all PENDING SELL orders for market, sorted by price ASC
        // 3. Find matches where buy.price >= sell.price
        // 4. For each match:
        //    - Calculate fill quantity (min of both quantities)
        //    - Update filledQuantity for both orders
        //    - Transfer funds between wallets
        //    - Update or create positions for both users
        //    - Create ORDER_EXECUTED transactions
        //    - Update order states (FILLED or PARTIALLY_FILLED)
        // 5. Return count of matches

        return 0; // Placeholder
    }

    /**
     * Executes a trade between two orders.
     * TODO: Implement trade execution logic
     * @param buyOrder The buy order
     * @param sellOrder The sell order
     * @param quantity The quantity to trade
     * @return true if successful
     */
    @Transactional
    protected boolean executeTrade(Order buyOrder, Order sellOrder, int quantity) {
        // TODO: Implement trade execution
        // 1. Update filledQuantity for both orders
        // 2. Calculate trade amount
        // 3. Transfer funds from buyer's locked balance to seller's balance
        // 4. Update/create positions for buyer (add shares)
        // 5. Update/create positions for seller (subtract shares)
        // 6. Create ORDER_EXECUTED transactions for both users
        // 7. Update order states based on fill status

        return false; // Placeholder
    }
}
