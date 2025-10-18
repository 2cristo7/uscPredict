package usc.uscPredict.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usc.uscPredict.model.*;
import usc.uscPredict.repository.EventRepository;
import usc.uscPredict.repository.MarketRepository;
import usc.uscPredict.repository.OrderRepository;

import java.math.BigDecimal;
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

    @Autowired
    public MarketService(MarketRepository marketRepository, EventRepository eventRepository, OrderRepository orderRepository) {
        this.marketRepository = marketRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
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
     * TODO: THIS IS YOUR MATCHING ENGINE - IMPLEMENT YOUR CUSTOM LOGIC HERE
     * Should find compatible BUY and SELL orders and execute trades.
     * @param marketId The market UUID
     * @return Number of matches executed
     */
    @Transactional
    public int matchOrders(UUID marketId) {
        // TODO: Implement order matching logic
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new IllegalArgumentException("Market ID does not exist"));

        // 1. Get all PENDING BUY orders for market, sorted by price DESC
        orderRepository.findByMarketIdAndState(marketId, OrderState.PENDING);




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
