package usc.uscPredict.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usc.uscPredict.model.*;
import usc.uscPredict.repository.EventRepository;
import usc.uscPredict.repository.MarketRepository;
import usc.uscPredict.repository.OrderRepository;
import usc.uscPredict.repository.PositionRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service layer for Market entity.
 * Handles business logic for market management.
 */
@Getter
@Service
public class MarketService {

    private final MarketRepository marketRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final PositionRepository positionRepository;

    @Autowired
    public MarketService(
            MarketRepository marketRepository,
            EventRepository eventRepository,
            OrderRepository orderRepository,
            WalletService walletService,
            TransactionService transactionService,
            PositionRepository positionRepository) {
        this.marketRepository = marketRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.positionRepository = positionRepository;
    }

    /**
     * Retrieves all markets in the system.
     * @return Set of all markets
     */
    public Set<Market> getAllMarkets() {
        return marketRepository.findAll();
    }

    /**
     * Retrieves a single market by its UUID.
     * @param uuid The market's unique identifier
     * @return The market if found, null otherwise
     */
    public Market getMarketById(UUID uuid) {
        Optional<Market> market = marketRepository.findById(uuid);
        return market.orElse(null);
    }

    /**
     * Creates a new market.
     * TODO: Add validations:
     * - Verify that the event exists
     * - Check for duplicate outcomes within the same event
     * - Validate that event is in OPEN state
     * @param market The market to create
     * @return The created market with generated UUID
     */
    @Transactional
    public Market createMarket(Market market) {

        Event event = eventRepository.findById(market.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event ID does not exist"));
        // - Verify eventId exists in database
        if (!eventRepository.existsById(market.getEventId())) {
            throw new IllegalArgumentException("Event ID does not exist");
        }
        // - Check event state allows new markets
        if (event.getState() != EventState.OPEN) {
            throw new IllegalArgumentException("Cannot create market for non-OPEN event");
        }
        // - Check for duplicate markets for the same event or outcome
        Set<Market> existingMarkets = marketRepository.findByEventId(market.getEventId());
        for (Market existing : existingMarkets) {
            if (existing.getOutcome().equalsIgnoreCase(market.getOutcome())) {
                throw new IllegalArgumentException("Duplicate market outcome for this event");
            }
        }

        return marketRepository.save(market);
    }

    /**
     * Updates an existing market.
     * TODO: Add validation (e.g., can't change eventId, can't modify settled markets)
     * @param uuid The UUID of the market to update
     * @param marketData The new market data
     * @return The updated market, or null if not found
     */
    @Transactional
    public Market updateMarket(UUID uuid, Market marketData) {
        Optional<Market> existingOpt = marketRepository.findById(uuid);
        if (existingOpt.isEmpty()) {
            return null;
        }

        Market existing = existingOpt.get();

        // TODO: Implement update logic
        // - Validate status transitions
        // - Don't allow changing eventId
        // - Don't allow changing outcome after orders exist
        existing.setOutcome(marketData.getOutcome());
        existing.setStatus(marketData.getStatus());

        return marketRepository.save(existing);
    }

    /**
     * Deletes a market by UUID.
     * TODO: Add safety checks (can't delete markets with orders/positions)
     * @param uuid The UUID of the market to delete
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean deleteMarket(UUID uuid) {
        if (marketRepository.existsById(uuid)) {
            // TODO: Verify no orders or positions exist for this market
            marketRepository.deleteById(uuid);
            return true;
        }
        return false;
    }

    /**
     * Retrieves all markets for a specific event.
     * @param eventId The event UUID
     * @return Set of markets belonging to the event
     */
    public Set<Market> getMarketsByEventId(UUID eventId) {
        return marketRepository.findByEventId(eventId);
    }

    /**
     * Retrieves all markets with a specific status.
     * @param status The market status to filter by
     * @return Set of markets with the given status
     */
    public Set<Market> getMarketsByStatus(MarketStatus status) {
        return marketRepository.findByStatus(status);
    }

    /**
     * Changes the status of a market.
     * TODO: Implement status transition validation
     * @param uuid The market UUID
     * @param newStatus The new status
     * @return The updated market, or null if not found
     */
    @Transactional
    public Market changeMarketStatus(UUID uuid, MarketStatus newStatus) {
        Optional<Market> marketOpt = marketRepository.findById(uuid);
        if (marketOpt.isEmpty()) {
            return null;
        }

        Market market = marketOpt.get();

        // TODO: Validate status transitions
        // ACTIVE -> SUSPENDED (valid)
        // SUSPENDED -> ACTIVE (valid)
        // ACTIVE -> SETTLED (valid, when event resolves)
        // SETTLED -> ACTIVE (invalid)
        market.setStatus(newStatus);

        return marketRepository.save(market);
    }

    /**
     * Settles a market with a winning outcome.
     * TODO: Implement settlement logic:
     * - Mark market as SETTLED
     * - Resolve all positions
     * - Distribute winnings
     * @param uuid The market UUID
     * @return The settled market, or null if not found
     */
    @Transactional
    public Market settleMarket(UUID uuid) {
        Optional<Market> marketOpt = marketRepository.findById(uuid);
        if (marketOpt.isEmpty()) {
            return null;
        }

        Market market = marketOpt.get();

        // TODO: Implement settlement logic
        // - Cancel all pending orders
        // - Calculate position payouts
        // - Update user wallets
        // - Create settlement transactions
        market.setStatus(MarketStatus.SETTLED);

        return marketRepository.save(market);
    }

    /**
     * Matches orders for a specific market.
     * Should find compatible BUY and SELL orders and execute trades.
     * @param marketId The market UUID
     * @return Number of matches executed
     */
    @Transactional
    public int matchOrders(UUID marketId) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new IllegalArgumentException("Market ID does not exist"));

        // 1. Get all PENDING BUY orders for market, sorted by price DESC
        ArrayList<Order> buyOrders = new ArrayList<>(orderRepository.findByMarketIdAndSideAndStateOrderByPriceDesc(
                marketId, OrderSide.BUY, OrderState.PENDING));

        // 2. Get all PENDING SELL orders for market, sorted by price ASC
        ArrayList<Order> sellOrders = new ArrayList<>(orderRepository.findByMarketIdAndSideAndStateOrderByPriceAsc(
                marketId, OrderSide.SELL, OrderState.PENDING));

        // Early exit if no orders on one of the sides
        if (buyOrders.isEmpty() || sellOrders.isEmpty()) {
            return 0;
        }

        // Utility variables
        int matchCount = 0;
        int buyIndex = 0;
        int sellIndex = 0;

        // 3. Find matches where buy.price >= sell.price
        while (buyIndex < buyOrders.size() && sellIndex < sellOrders.size()) {
            Order buyOrder = buyOrders.get(buyIndex);
            Order sellOrder = sellOrders.get(sellIndex);

            // Check if match is possible
            if (buyOrder.getPrice().compareTo(sellOrder.getPrice()) < 0) {
                break;
            }

            // 4. Execute trade

            // 4.1 Determine fill quantity
            int buyRemaining = buyOrder.getQuantity() - buyOrder.getFilledQuantity();
            int sellRemaining = sellOrder.getQuantity() - sellOrder.getFilledQuantity();
            int quantityToFill = Math.min(buyRemaining, sellRemaining);

            // 4.2 Determine execution price (use maker price)
            BigDecimal executionPrice = (buyOrder.getCreatedAt().isBefore(sellOrder.getCreatedAt()))
                    ? buyOrder.getPrice() : sellOrder.getPrice();

            // 4.3 Calculate trade amount (in currency)
            BigDecimal tradeAmount = executionPrice.multiply(BigDecimal.valueOf(quantityToFill));

            // 4.4 Consume funds from both parties (both are buying their respective shares)
            // YES buyer pays execution price for YES shares
            // NO buyer pays (1 - execution price) for NO shares

            // 4.4.1 Calculate and consume YES buyer's payment
            BigDecimal buyerPayment = executionPrice.multiply(BigDecimal.valueOf(quantityToFill));
            walletService.consumeLockedFunds(buyOrder.getUserId(), buyerPayment);

            // 4.4.2 Refund YES buyer if they locked more than needed
            BigDecimal buyerLockedForThisFill = buyOrder.getPrice()
                    .multiply(BigDecimal.valueOf(quantityToFill));
            BigDecimal buyerRefund = buyerLockedForThisFill.subtract(buyerPayment);
            if (buyerRefund.compareTo(BigDecimal.ZERO) > 0) {
                walletService.unlockFunds(buyOrder.getUserId(), buyerRefund);
            }

            // 4.4.3 Calculate and consume NO buyer's payment
            BigDecimal sellerPayment = BigDecimal.ONE.subtract(executionPrice)
                    .multiply(BigDecimal.valueOf(quantityToFill));
            walletService.consumeLockedFunds(sellOrder.getUserId(), sellerPayment);

            // 4.4.4 Refund NO buyer if they locked more than needed
            BigDecimal sellerLockedForThisFill = BigDecimal.ONE.subtract(sellOrder.getPrice())
                    .multiply(BigDecimal.valueOf(quantityToFill));
            BigDecimal sellerRefund = sellerLockedForThisFill.subtract(sellerPayment);
            if (sellerRefund.compareTo(BigDecimal.ZERO) > 0) {
                walletService.unlockFunds(sellOrder.getUserId(), sellerRefund);
            }

            // 4.5 Update filled quantities
            buyOrder.setFilledQuantity(buyOrder.getFilledQuantity() + quantityToFill);
            sellOrder.setFilledQuantity(sellOrder.getFilledQuantity() + quantityToFill);

            // 4.6 Update order states
            if (buyOrder.getFilledQuantity().equals(buyOrder.getQuantity())) {
                buyOrder.setState(OrderState.FILLED);
            } else {
                buyOrder.setState(OrderState.PARTIALLY_FILLED);
            }

            if (sellOrder.getFilledQuantity().equals(sellOrder.getQuantity())) {
                sellOrder.setState(OrderState.FILLED);
            } else {
                sellOrder.setState(OrderState.PARTIALLY_FILLED);
            }

            // 4.7 Save updated orders
            orderRepository.save(buyOrder);
            orderRepository.save(sellOrder);

            // 4.8 Create ORDER_EXECUTED transactions for both users
            Transaction buyTransaction = new Transaction(
                    buyOrder.getUserId(),
                    TransactionType.ORDER_EXECUTED,
                    tradeAmount
            );

            Transaction sellTransaction = new Transaction(
                    sellOrder.getUserId(),
                    TransactionType.ORDER_EXECUTED,
                    tradeAmount
            );

            transactionService.createTransaction(buyTransaction);
            transactionService.createTransaction(sellTransaction);

            // 4.9 Update or create positions
            updatePosition(buyOrder.getUserId(), marketId, quantityToFill, true);
            updatePosition(sellOrder.getUserId(), marketId, quantityToFill, false);

            // 4.10 Increment match count
            matchCount++;

            // 5 Advance indices
            if (buyOrder.getState() == OrderState.FILLED) {
                buyIndex++;
            }

            if (sellOrder.getState() == OrderState.FILLED) {
                sellIndex++;
            }

        }

        return matchCount;
    }

    /**
     * Updates or creates a position for a user in a market.
     * Adds shares to either YES or NO based on the order side.
     *
     * @param userId The user UUID
     * @param marketId The market UUID
     * @param quantity The number of shares to add
     * @param isYes True if buying YES shares, false if buying NO shares
     */
    private void updatePosition(UUID userId, UUID marketId, int quantity, boolean isYes) {
        Position position = positionRepository
                .findByUserIdAndMarketId(userId, marketId)
                .orElse(new Position(userId, marketId));

        // Update appropriate share type
        if (isYes) {
            position.setYesShares(position.getYesShares() + quantity);
        } else {
            position.setNoShares(position.getNoShares() + quantity);
        }

        positionRepository.save(position);
    }
}
