package usc.uscPredict.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usc.uscPredict.model.*;
import usc.uscPredict.repository.MarketRepository;
import usc.uscPredict.repository.OrderRepository;
import usc.uscPredict.repository.UserRepository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service responsible for the transactional persistence of orders.
 *
 * DESIGN DECISION: Separation from OrderService
 * =============================================
 * This service exists as a separate class to enable proper Spring @Transactional behavior.
 *
 * WHY THIS SEPARATION IS NECESSARY:
 * ---------------------------------
 * Spring's @Transactional annotation works through AOP proxies. When a method calls another
 * @Transactional method in the SAME class (self-invocation), Spring's proxy is bypassed and
 * the transaction is NOT created.
 *
 * PROBLEM WITH SELF-INVOCATION:
 *   OrderService.createOrder() {
 *       this.createOrderTransaction(); // ❌ NO transaction created!
 *   }
 *
 * SOLUTION WITH SEPARATE SERVICE:
 *   OrderService.createOrder() {
 *       orderPersistenceService.persistOrder(); // ✅ Transaction created!
 *   }
 *
 * TRANSACTIONAL BOUNDARIES:
 * ------------------------
 * This service defines ONE atomic transaction:
 * 1. Validate user and market exist
 * 2. Calculate required funds
 * 3. Lock funds in wallet
 * 4. Save order as PENDING
 * 5. Create ORDER_PLACED transaction record
 *
 * If ANY step fails, the ENTIRE operation rolls back.
 *
 * ORDER MATCHING IS INTENTIONALLY EXCLUDED:
 * -----------------------------------------
 * Order matching (via MarketService.matchOrders()) is executed in a SEPARATE transaction
 * by OrderService. This ensures that:
 * - If order persistence fails → Order is NOT created
 * - If order matching fails → Order REMAINS as PENDING (can be matched later)
 *
 * This separation prevents the scenario where a valid order is lost due to a matching error.
 *
 * @see OrderService#createOrder(Order) - Orchestrates persistence and matching
 * @see MarketService#matchOrders(UUID) - Executes order matching in separate transaction
 */
@Service
public class OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final WalletService walletService;

    @Autowired
    public OrderPersistenceService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            MarketRepository marketRepository,
            WalletService walletService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.marketRepository = marketRepository;
        this.walletService = walletService;
    }

    /**
     * Persists a new order to the database within a single atomic transaction.
     *
     * TRANSACTION SCOPE:
     * -----------------
     * This method executes the following steps atomically:
     * 1. User validation
     * 2. Market validation
     * 3. Fund calculation (based on order side: BUY or SELL)
     * 4. Wallet balance check
     * 5. Fund locking
     * 6. Order persistence (state = PENDING)
     * 7. Transaction record creation
     *
     * If any step fails, the entire transaction is rolled back and no database changes occur.
     *
     * FUND CALCULATION LOGIC:
     * ----------------------
     * - BUY orders (YES shares): User pays price × quantity
     * - SELL orders (NO shares): User pays (1 - price) × quantity
     *
     * Since YES price + NO price = 1 in prediction markets, a SELL order at price 0.6
     * means buying NO shares at price 0.4.
     *
     * POST-CONDITIONS:
     * ---------------
     * On success:
     * - Order exists in database with state PENDING
     * - User's wallet has locked funds equal to requiredFunds
     * - ORDER_PLACED transaction record exists
     *
     * On failure:
     * - No database changes (complete rollback)
     * - Exception thrown with descriptive error message
     *
     * @param order The order to persist (uuid will be generated)
     * @return The persisted order with generated UUID
     * @throws IllegalArgumentException if user or market not found
     * @throws IllegalStateException if insufficient wallet balance
     */
    @Transactional
    public Order persistOrder(Order order) {
        // Step 1: Validate user exists
        if (!userRepository.existsById(order.getUserId())) {
            throw new IllegalArgumentException("User not found with ID: " + order.getUserId());
        }

        // Step 2: Validate market exists
        if (!marketRepository.existsById(order.getMarketId())) {
            throw new IllegalArgumentException("Market not found with ID: " + order.getMarketId());
        }

        // Step 3: Calculate required funds based on order side
        BigDecimal requiredFunds;

        if (order.getSide() == OrderSide.BUY) {
            // BUY: User is buying YES shares, pays price * quantity
            // Example: BUY 100 shares at $0.60 = $60
            requiredFunds = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        } else {
            // SELL: User is buying NO shares, pays (1 - price) * quantity
            // Since YES price + NO price = 1, NO price = 1 - YES price
            // Example: SELL at YES price $0.60 means buying NO at $0.40
            //          100 shares × $0.40 = $40
            BigDecimal noPrice = BigDecimal.ONE.subtract(order.getPrice());
            requiredFunds = noPrice.multiply(BigDecimal.valueOf(order.getQuantity()));
        }

        // Step 4: Check wallet has sufficient balance
        Wallet wallet = walletService.getWalletByUserId(order.getUserId());

        if (wallet.getBalance().compareTo(requiredFunds) < 0) {
            throw new IllegalStateException(
                    String.format("Insufficient balance. Available: %s, Required: %s",
                            wallet.getBalance(), requiredFunds)
            );
        }

        // Step 5: Lock funds in wallet
        // This moves funds from available balance to locked balance
        walletService.lockFunds(order.getUserId(), requiredFunds);

        // Step 6: Set order state to PENDING
        // The order will remain PENDING until matched by MarketService
        order.setState(OrderState.PENDING);

        // Step 7: Create ORDER_PLACED transaction record
        // This creates an audit trail of the order placement
        Transaction transaction = new Transaction(
                order.getUserId(),
                TransactionType.ORDER_PLACED,
                requiredFunds
        );
        walletService.getTransactionService().createTransaction(transaction);

        // Step 8: Save and return order
        // At this point, all validations passed and funds are locked
        return orderRepository.save(order);
    }
}
