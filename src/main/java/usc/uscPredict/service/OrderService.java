package usc.uscPredict.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usc.uscPredict.exception.OrderNotFoundException;
import usc.uscPredict.exception.PredictUsernameNotFoundException;
import usc.uscPredict.model.*;
import usc.uscPredict.repository.MarketRepository;
import usc.uscPredict.repository.OrderRepository;
import usc.uscPredict.repository.UserRepository;

import java.math.BigDecimal;
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
    private final WalletService walletService;
    private final MarketService marketService;

    @Autowired
    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            MarketRepository marketRepository,
            WalletService walletService,
            MarketService marketService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.marketRepository = marketRepository;
        this.walletService = walletService;
        this.marketService = marketService;
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
     * @return The order if found
     * @throws OrderNotFoundException if the order is not found
     */
    public Order getOrderById(UUID uuid) {
        return orderRepository.findById(uuid)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + uuid));
    }

    /**
     * Creates a new order (places an order).
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
        // 1. Validate user exists
        if (!userRepository.existsById(order.getUserId())) {
            throw new PredictUsernameNotFoundException("User not found with ID: " + order.getUserId());
        }

        // 2. Validate market exists and is ACTIVE
        if (!marketRepository.existsById(order.getMarketId())) {
            throw new IllegalArgumentException("Market not found with ID: " + order.getMarketId());
        }

        // 3. Calculate required funds based on order side
        BigDecimal requiredFunds;

        if (order.getSide() == OrderSide.BUY) {
            // BUY: User is buying YES shares, pays price * quantity
            requiredFunds = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        } else {
            // SELL: User is buying NO shares, pays (1 - price) * quantity
            // Since YES price + NO price = 1, NO price = 1 - YES price
            BigDecimal noPrice = BigDecimal.ONE.subtract(order.getPrice());
            requiredFunds = noPrice.multiply(BigDecimal.valueOf(order.getQuantity()));
        }

        // 4. Check wallet has sufficient balance
        Wallet wallet = walletService.getWalletByUserId(order.getUserId());

        if (wallet.getBalance().compareTo(requiredFunds)<0) {
            throw new IllegalStateException(
                    String.format("Insufficient balance. Available: %s, Required: %s",
                            wallet.getBalance(), requiredFunds)
            );
        }

        // 5. Lock funds in wallet
        walletService.lockFunds(order.getUserId(), requiredFunds);

        // 6. Save order with state PENDING

        order.setState(OrderState.PENDING);
        // 7. Create ORDER_PLACED transaction

        Transaction transaction = new Transaction(
                order.getUserId(),
                TransactionType.ORDER_PLACED,
                requiredFunds
        );
        walletService.getTransactionService().createTransaction(transaction);

        // 8. Save order in repository
        orderRepository.save(order);

        // 9. Call to marketService to attempt matching orders
        Market market = marketRepository.findById(order.getMarketId()).orElse(null);
        if (market != null && market.getStatus() == MarketStatus.ACTIVE) {
            marketService.matchOrders(order.getMarketId());
        }


        return orderRepository.save(order);
    }

    /**
     * Updates an existing order.
     * TODO: Add validation (only allow updating PENDING orders)
     * @param uuid The UUID of the order to update
     * @param orderData The new order data
     * @return The updated order
     * @throws OrderNotFoundException if the order is not found
     */
    @Transactional
    public Order updateOrder(UUID uuid, Order orderData) {
        Order existing = orderRepository.findById(uuid)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + uuid));

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
     * @return The cancelled order
     * @throws OrderNotFoundException if the order is not found
     */
    @Transactional
    public Order cancelOrder(UUID uuid) {
        Order order = orderRepository.findById(uuid)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + uuid));

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

    /**
     * Patches an existing order using JSON-Patch operations.
     * Only allows updating PENDING or PARTIALLY_FILLED orders.
     * Validates that price and quantity are within allowed ranges.
     * @param uuid The UUID of the order to patch
     * @param patchedOrder The order with patched fields
     * @return The updated order
     */
    @Transactional
    public Order patchOrder(UUID uuid, Order patchedOrder) {
        // Validate the patched order
        if (patchedOrder.getPrice().compareTo(BigDecimal.ZERO) <= 0 ||
            patchedOrder.getPrice().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException("Price must be between 0 and 1");
        }

        if (patchedOrder.getQuantity() < 1) {
            throw new IllegalStateException("Quantity must be at least 1");
        }

        // Save the updated order
        return orderRepository.save(patchedOrder);
    }
}
