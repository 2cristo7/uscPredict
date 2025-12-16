package usc.uscPredict.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usc.uscPredict.exception.EventNotFoundException;
import usc.uscPredict.exception.MarketNotFoundException;
import usc.uscPredict.model.*;
import usc.uscPredict.repository.EventRepository;
import usc.uscPredict.repository.MarketRepository;
import usc.uscPredict.repository.OrderRepository;
import usc.uscPredict.repository.PositionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for Market entity.
 * Handles business logic for market management.
 */
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
     * @return The market if found
     * @throws MarketNotFoundException if the market is not found
     */
    public Market getMarketById(UUID uuid) {
        return marketRepository.findById(uuid)
                .orElseThrow(() -> new MarketNotFoundException("Market not found with ID: " + uuid));
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
                .orElseThrow(() -> new EventNotFoundException("Event not found with ID: " + market.getEventId()));

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
     * - Cancels all pending orders and refunds locked funds
     * - Pays out winning positions ($1 per share)
     * - Creates settlement transactions
     * - Clears positions
     * - Marks market as SETTLED
     *
     * @param uuid The market UUID
     * @param winningOutcome "YES" or "NO"
     * @return The settled market, or null if not found
     */
    @Transactional
    public Market settleMarket(UUID uuid, String winningOutcome) {
        Optional<Market> marketOpt = marketRepository.findById(uuid);
        if (marketOpt.isEmpty()) {
            return null;
        }

        Market market = marketOpt.get();

        // Validate winning outcome
        boolean yesWins = "YES".equalsIgnoreCase(winningOutcome);
        boolean noWins = "NO".equalsIgnoreCase(winningOutcome);
        if (!yesWins && !noWins) {
            throw new IllegalArgumentException("Winning outcome must be YES or NO");
        }

        // 1. Cancel all pending/partially filled orders and refund locked funds
        Set<Order> activeOrders = orderRepository.findActiveOrdersByMarketId(uuid);
        for (Order order : activeOrders) {
            int unfilledQuantity = order.getQuantity() - order.getFilledQuantity();
            if (unfilledQuantity > 0) {
                // Calculate locked amount for unfilled quantity
                BigDecimal lockedAmount;
                if (order.getSide() == OrderSide.BUY) {
                    lockedAmount = order.getPrice().multiply(BigDecimal.valueOf(unfilledQuantity));
                } else {
                    // SELL orders lock (1 - price) per share
                    lockedAmount = BigDecimal.ONE.subtract(order.getPrice())
                            .multiply(BigDecimal.valueOf(unfilledQuantity));
                }

                // Refund locked funds
                walletService.unlockFunds(order.getUserId(), lockedAmount);

                // Create ORDER_CANCELLED transaction
                Transaction cancelTx = new Transaction(
                        order.getUserId(),
                        TransactionType.ORDER_CANCELLED,
                        lockedAmount
                );
                transactionService.createTransaction(cancelTx);
            }

            // Mark order as cancelled
            order.setState(OrderState.CANCELLED);
            orderRepository.save(order);
        }

        // 2. Find all positions for this market and pay out winners
        Set<Position> positions = positionRepository.findByMarketId(uuid);
        for (Position position : positions) {
            BigDecimal payout = BigDecimal.ZERO;

            if (yesWins && position.getYesShares() > 0) {
                // YES wins: Each YES share pays $1
                payout = BigDecimal.valueOf(position.getYesShares());
            } else if (noWins && position.getNoShares() > 0) {
                // NO wins: Each NO share pays $1
                payout = BigDecimal.valueOf(position.getNoShares());
            }

            if (payout.compareTo(BigDecimal.ZERO) > 0) {
                // Add payout to user's wallet
                Wallet wallet = walletService.getWalletByUserId(position.getUserId());
                wallet.setBalance(wallet.getBalance().add(payout));

                // Create SETTLEMENT transaction
                Transaction settlementTx = new Transaction(
                        position.getUserId(),
                        TransactionType.SETTLEMENT,
                        payout
                );
                transactionService.createTransaction(settlementTx);
            }

            // Clear position shares (market is settled)
            position.setYesShares(0);
            position.setNoShares(0);
            positionRepository.save(position);
        }

        // 3. Mark market as SETTLED
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

        // 1. Get all active BUY orders (PENDING + PARTIALLY_FILLED) for market, sorted by price DESC
        ArrayList<Order> buyOrders = new ArrayList<>(orderRepository.findActiveOrdersByMarketIdAndSideOrderByPriceDesc(
                marketId, OrderSide.BUY));

        // 2. Get all active SELL orders (PENDING + PARTIALLY_FILLED) for market, sorted by price ASC
        ArrayList<Order> sellOrders = new ArrayList<>(orderRepository.findActiveOrdersByMarketIdAndSideOrderByPriceAsc(
                marketId, OrderSide.SELL));

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

            // 4.5 Set execution price on both orders
            buyOrder.setExecutionPrice(executionPrice);
            sellOrder.setExecutionPrice(executionPrice);

            // 4.5.1 Update market's last traded price
            market.setLastPrice(executionPrice);
            marketRepository.save(market);

            // 4.6 Update filled quantities
            buyOrder.setFilledQuantity(buyOrder.getFilledQuantity() + quantityToFill);
            sellOrder.setFilledQuantity(sellOrder.getFilledQuantity() + quantityToFill);

            // 4.7 Update order states
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

            // 4.9 Update or create positions with execution price for avgCost
            updatePosition(buyOrder.getUserId(), marketId, quantityToFill, true, executionPrice);
            updatePosition(sellOrder.getUserId(), marketId, quantityToFill, false, executionPrice);

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
     * Calculates weighted average cost for the position.
     *
     * @param userId The user UUID
     * @param marketId The market UUID
     * @param quantity The number of shares to add
     * @param isYes True if buying YES shares, false if buying NO shares
     * @param executionPrice The price at which the trade was executed
     */
    private void updatePosition(UUID userId, UUID marketId, int quantity, boolean isYes, BigDecimal executionPrice) {
        Position position = positionRepository
                .findByUserIdAndMarketId(userId, marketId)
                .orElse(new Position(userId, marketId));

        // Update appropriate share type and calculate weighted average cost
        if (isYes) {
            int oldShares = position.getYesShares();
            BigDecimal oldAvgCost = position.getAvgYesCost() != null ? position.getAvgYesCost() : BigDecimal.ZERO;

            // Weighted average: (oldShares * oldAvg + newShares * newPrice) / totalShares
            BigDecimal totalCost = oldAvgCost.multiply(BigDecimal.valueOf(oldShares))
                    .add(executionPrice.multiply(BigDecimal.valueOf(quantity)));
            int totalShares = oldShares + quantity;
            BigDecimal newAvgCost = totalCost.divide(BigDecimal.valueOf(totalShares), 4, java.math.RoundingMode.HALF_UP);

            position.setYesShares(totalShares);
            position.setAvgYesCost(newAvgCost);
        } else {
            int oldShares = position.getNoShares();
            BigDecimal oldAvgCost = position.getAvgNoCost() != null ? position.getAvgNoCost() : BigDecimal.ZERO;

            // NO buyer pays (1 - executionPrice) per share
            BigDecimal noCost = BigDecimal.ONE.subtract(executionPrice);
            BigDecimal totalCost = oldAvgCost.multiply(BigDecimal.valueOf(oldShares))
                    .add(noCost.multiply(BigDecimal.valueOf(quantity)));
            int totalShares = oldShares + quantity;
            BigDecimal newAvgCost = totalCost.divide(BigDecimal.valueOf(totalShares), 4, java.math.RoundingMode.HALF_UP);

            position.setNoShares(totalShares);
            position.setAvgNoCost(newAvgCost);
        }

        positionRepository.save(position);
    }

    /**
     * Calculates the total trading volume for a market.
     * Volume = sum of (executionPrice * filledQuantity) for all executed BUY orders.
     * We only count BUY orders to avoid double-counting (each trade has a BUY and SELL).
     *
     * @param marketId The market UUID
     * @return Total volume in currency units
     */
    public BigDecimal getMarketVolume(UUID marketId) {
        List<Order> executedOrders = orderRepository.findExecutedOrdersByMarketIdOrderByUpdatedAt(marketId);

        // Only count BUY orders to avoid double-counting
        return executedOrders.stream()
                .filter(o -> o.getSide() == OrderSide.BUY)
                .map(o -> {
                    BigDecimal price = o.getExecutionPrice() != null ? o.getExecutionPrice() : o.getPrice();
                    return price.multiply(BigDecimal.valueOf(o.getFilledQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * DTO for price history data points.
     */
    @Getter
    public static class PricePoint {
        private final LocalDateTime timestamp;
        private final BigDecimal price;

        public PricePoint(LocalDateTime timestamp, BigDecimal price) {
            this.timestamp = timestamp;
            this.price = price;
        }
    }

    /**
     * Gets the price history for a market, bucketed by time intervals.
     * Returns execution prices from filled orders, grouped into time buckets.
     *
     * @param marketId The market UUID
     * @param bucketMinutes The size of each time bucket in minutes
     * @return List of PricePoint representing price over time
     */
    public List<PricePoint> getPriceHistory(UUID marketId, int bucketMinutes) {
        List<Order> executedOrders = orderRepository.findExecutedOrdersByMarketIdOrderByUpdatedAt(marketId);

        if (executedOrders.isEmpty()) {
            return Collections.emptyList();
        }

        // Group by time bucket and calculate average price per bucket
        Map<LocalDateTime, List<Order>> buckets = executedOrders.stream()
                .collect(Collectors.groupingBy(order -> {
                    LocalDateTime time = order.getUpdatedAt();
                    long minutesSinceEpoch = time.toEpochSecond(java.time.ZoneOffset.UTC) / 60;
                    long bucketStart = (minutesSinceEpoch / bucketMinutes) * bucketMinutes;
                    return LocalDateTime.ofEpochSecond(bucketStart * 60, 0, java.time.ZoneOffset.UTC);
                }));

        // Convert to PricePoints with average price per bucket
        return buckets.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    BigDecimal avgPrice = entry.getValue().stream()
                            .map(Order::getExecutionPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(entry.getValue().size()), 4, java.math.RoundingMode.HALF_UP);
                    return new PricePoint(entry.getKey(), avgPrice);
                })
                .collect(Collectors.toList());
    }
}
