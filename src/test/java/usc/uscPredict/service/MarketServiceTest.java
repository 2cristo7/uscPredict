package usc.uscPredict.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import usc.uscPredict.model.*;
import usc.uscPredict.repository.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the YES/NO shares market system.
 * Tests the complete flow: order creation, matching, position updates, and fund transfers.
 */
@SpringBootTest
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
class MarketServiceTest {

    @Autowired
    private MarketService marketService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MarketRepository marketRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User user1;
    private User user2;
    private Event event;
    private Market market;
    private Wallet wallet1;
    private Wallet wallet2;

    @BeforeEach
    void setUp() {
        // Clean up
        orderRepository.deleteAll();
        positionRepository.deleteAll();
        walletRepository.deleteAll();
        marketRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        user1 = new User("alice", "alice@test.com", "hash1", Role.USER);
        user1 = userRepository.save(user1);

        user2 = new User("bob", "bob@test.com", "hash2", Role.USER);
        user2 = userRepository.save(user2);

        // Create wallets with initial balance
        wallet1 = new Wallet(user1.getUuid(), new BigDecimal("1000.00"), BigDecimal.ZERO);
        wallet1 = walletRepository.save(wallet1);

        wallet2 = new Wallet(user2.getUuid(), new BigDecimal("1000.00"), BigDecimal.ZERO);
        wallet2 = walletRepository.save(wallet2);

        // Create event
        event = new Event("Will it rain tomorrow?", "Prediction for rain", EventState.OPEN);
        event = eventRepository.save(event);

        // Create market
        market = new Market(event.getUuid(), "YES", MarketStatus.ACTIVE);
        market = marketRepository.save(market);
    }

    @Test
    void testYesSharePurchase() {
        // User1 buys YES shares at $0.60
        Order buyOrder = new Order();
        buyOrder.setUserId(user1.getUuid());
        buyOrder.setMarketId(market.getUuid());
        buyOrder.setSide(OrderSide.BUY);
        buyOrder.setPrice(new BigDecimal("0.60"));
        buyOrder.setQuantity(100);
        buyOrder.setFilledQuantity(0);

        orderService.createOrder(buyOrder);

        // Check wallet: should lock 100 * 0.60 = $60
        Wallet updatedWallet = walletService.getWalletByUserId(user1.getUuid());
        assertEquals(0, new BigDecimal("940.00").compareTo(updatedWallet.getBalance()));
        assertEquals(0, new BigDecimal("60.00").compareTo(updatedWallet.getLockedBalance()));
    }

    @Test
    void testNoSharePurchase() {
        // User1 buys NO shares (SELL order) at YES price $0.60 → NO price $0.40
        Order sellOrder = new Order();
        sellOrder.setUserId(user1.getUuid());
        sellOrder.setMarketId(market.getUuid());
        sellOrder.setSide(OrderSide.SELL);
        sellOrder.setPrice(new BigDecimal("0.60")); // YES price
        sellOrder.setQuantity(100);
        sellOrder.setFilledQuantity(0);

        orderService.createOrder(sellOrder);

        // Check wallet: should lock 100 * (1 - 0.60) = 100 * 0.40 = $40
        Wallet updatedWallet = walletService.getWalletByUserId(user1.getUuid());
        assertEquals(0, new BigDecimal("960.00").compareTo(updatedWallet.getBalance()));
        assertEquals(0, new BigDecimal("40.00").compareTo(updatedWallet.getLockedBalance()));
    }

    @Test
    void testOrderMatching_ExactPriceMatch() {
        // User1 buys YES at $0.60 (100 shares)
        Order buyOrder = new Order();
        buyOrder.setUserId(user1.getUuid());
        buyOrder.setMarketId(market.getUuid());
        buyOrder.setSide(OrderSide.BUY);
        buyOrder.setPrice(new BigDecimal("0.60"));
        buyOrder.setQuantity(100);
        buyOrder.setFilledQuantity(0);
        buyOrder = orderService.createOrder(buyOrder);

        // User2 buys NO at YES price $0.60 (which means NO price $0.40)
        Order sellOrder = new Order();
        sellOrder.setUserId(user2.getUuid());
        sellOrder.setMarketId(market.getUuid());
        sellOrder.setSide(OrderSide.SELL);
        sellOrder.setPrice(new BigDecimal("0.60"));
        sellOrder.setQuantity(100);
        sellOrder.setFilledQuantity(0);
        sellOrder = orderService.createOrder(sellOrder);

        // Verify both orders are filled
        Order updatedBuy = orderService.getOrderById(buyOrder.getUuid());
        Order updatedSell = orderService.getOrderById(sellOrder.getUuid());

        assertEquals(OrderState.FILLED, updatedBuy.getState());
        assertEquals(OrderState.FILLED, updatedSell.getState());
        assertEquals(100, updatedBuy.getFilledQuantity());
        assertEquals(100, updatedSell.getFilledQuantity());

        // Verify positions
        Position position1 = positionRepository
                .findByUserIdAndMarketId(user1.getUuid(), market.getUuid())
                .orElseThrow();
        Position position2 = positionRepository
                .findByUserIdAndMarketId(user2.getUuid(), market.getUuid())
                .orElseThrow();

        assertEquals(100, position1.getYesShares());
        assertEquals(0, position1.getNoShares());
        assertEquals(0, position2.getYesShares());
        assertEquals(100, position2.getNoShares());

        // Verify wallets: both should have spent their locked amounts
        Wallet updatedWallet1 = walletService.getWalletByUserId(user1.getUuid());
        Wallet updatedWallet2 = walletService.getWalletByUserId(user2.getUuid());

        // User1 paid $60 for YES shares
        assertEquals(0, new BigDecimal("940.00").compareTo(updatedWallet1.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedWallet1.getLockedBalance()));

        // User2 paid $40 for NO shares
        assertEquals(0, new BigDecimal("960.00").compareTo(updatedWallet2.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedWallet2.getLockedBalance()));
    }

    @Test
    void testOrderMatching_WithRefund() {
        // User1 buys YES at $0.70 (willing to pay more)
        Order buyOrder = new Order();
        buyOrder.setUserId(user1.getUuid());
        buyOrder.setMarketId(market.getUuid());
        buyOrder.setSide(OrderSide.BUY);
        buyOrder.setPrice(new BigDecimal("0.70"));
        buyOrder.setQuantity(100);
        buyOrder.setFilledQuantity(0);
        buyOrder = orderService.createOrder(buyOrder);

        // User2 buys NO at YES price $0.60 (NO price $0.40)
        Order sellOrder = new Order();
        sellOrder.setUserId(user2.getUuid());
        sellOrder.setMarketId(market.getUuid());
        sellOrder.setSide(OrderSide.SELL);
        sellOrder.setPrice(new BigDecimal("0.60"));
        sellOrder.setQuantity(100);
        sellOrder.setFilledQuantity(0);
        sellOrder = orderService.createOrder(sellOrder);

        // Verify both orders are filled
        Order updatedBuy = orderService.getOrderById(buyOrder.getUuid());
        Order updatedSell = orderService.getOrderById(sellOrder.getUuid());

        assertEquals(OrderState.FILLED, updatedBuy.getState());
        assertEquals(OrderState.FILLED, updatedSell.getState());

        // Execution price should be $0.70 (buyer's price, since buyer was maker)
        // User1 pays $70, User2 pays $30
        Wallet updatedWallet1 = walletService.getWalletByUserId(user1.getUuid());
        Wallet updatedWallet2 = walletService.getWalletByUserId(user2.getUuid());

        // User1 locked $70, paid $70, no refund
        assertEquals(0, new BigDecimal("930.00").compareTo(updatedWallet1.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedWallet1.getLockedBalance()));

        // User2 locked $40 (1-0.60)*100, but only needed $30 (1-0.70)*100, so gets $10 refund
        assertEquals(0, new BigDecimal("970.00").compareTo(updatedWallet2.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedWallet2.getLockedBalance()));
    }

    @Test
    void testPartialFill() {
        // User1 wants to buy 100 YES shares at $0.60
        Order buyOrder = new Order();
        buyOrder.setUserId(user1.getUuid());
        buyOrder.setMarketId(market.getUuid());
        buyOrder.setSide(OrderSide.BUY);
        buyOrder.setPrice(new BigDecimal("0.60"));
        buyOrder.setQuantity(100);
        buyOrder.setFilledQuantity(0);
        buyOrder = orderService.createOrder(buyOrder);

        // User2 only sells 50 NO shares
        Order sellOrder = new Order();
        sellOrder.setUserId(user2.getUuid());
        sellOrder.setMarketId(market.getUuid());
        sellOrder.setSide(OrderSide.SELL);
        sellOrder.setPrice(new BigDecimal("0.60"));
        sellOrder.setQuantity(50);
        sellOrder.setFilledQuantity(0);
        sellOrder = orderService.createOrder(sellOrder);

        // Verify states
        Order updatedBuy = orderService.getOrderById(buyOrder.getUuid());
        Order updatedSell = orderService.getOrderById(sellOrder.getUuid());

        assertEquals(OrderState.PARTIALLY_FILLED, updatedBuy.getState());
        assertEquals(OrderState.FILLED, updatedSell.getState());
        assertEquals(50, updatedBuy.getFilledQuantity());
        assertEquals(50, updatedSell.getFilledQuantity());

        // Verify positions
        Position position1 = positionRepository
                .findByUserIdAndMarketId(user1.getUuid(), market.getUuid())
                .orElseThrow();
        Position position2 = positionRepository
                .findByUserIdAndMarketId(user2.getUuid(), market.getUuid())
                .orElseThrow();

        assertEquals(50, position1.getYesShares());
        assertEquals(0, position1.getNoShares());
        assertEquals(0, position2.getYesShares());
        assertEquals(50, position2.getNoShares());

        // User1 locked $60, consumed $30, still has $30 locked
        Wallet updatedWallet1 = walletService.getWalletByUserId(user1.getUuid());
        // Total should be $1000: balance $940 + locked $30 + consumed $30 = $1000
        BigDecimal expectedBalance = new BigDecimal("940.00"); // $1000 - $60 locked = $940
        BigDecimal expectedLocked = new BigDecimal("30.00");   // $60 - $30 consumed = $30

        // Assert balance ($1000 start - $60 locked = $940 available)
        assertTrue(expectedBalance.compareTo(updatedWallet1.getBalance()) == 0,
                String.format("Expected balance %s but got %s", expectedBalance, updatedWallet1.getBalance()));

        // Assert locked ($60 initially locked - $30 consumed = $30 still locked for remaining 50 shares)
        assertTrue(expectedLocked.compareTo(updatedWallet1.getLockedBalance()) == 0,
                String.format("Expected locked %s but got %s", expectedLocked, updatedWallet1.getLockedBalance()));
    }

    @Test
    void testConservationOfProbability() {
        // YES price + NO price should always equal $1
        BigDecimal yesPrice = new BigDecimal("0.65");
        BigDecimal noPrice = BigDecimal.ONE.subtract(yesPrice);

        assertEquals(0, new BigDecimal("0.35").compareTo(noPrice));
        assertEquals(0, BigDecimal.ONE.compareTo(yesPrice.add(noPrice)));
    }

    @Test
    void testNetExposure() {
        // Create positions manually for testing helper methods
        Position position = new Position(user1.getUuid(), market.getUuid());
        position.setYesShares(100);
        position.setNoShares(30);
        position = positionRepository.save(position);

        // Total shares = 130
        assertEquals(130, position.getTotalShares());

        // Net exposure = 100 - 30 = +70 (bullish on YES)
        assertEquals(70, position.getNetExposure());

        // Has both types of shares
        assertTrue(position.hasYesShares());
        assertTrue(position.hasNoShares());
    }

    @Test
    void testHedgedPosition() {
        // User buys both YES and NO to hedge
        Order buyYes = new Order();
        buyYes.setUserId(user1.getUuid());
        buyYes.setMarketId(market.getUuid());
        buyYes.setSide(OrderSide.BUY);
        buyYes.setPrice(new BigDecimal("0.60"));
        buyYes.setQuantity(50);
        buyYes.setFilledQuantity(0);
        buyYes = orderService.createOrder(buyYes);

        Order buyNo = new Order();
        buyNo.setUserId(user1.getUuid());
        buyNo.setMarketId(market.getUuid());
        buyNo.setSide(OrderSide.SELL);
        buyNo.setPrice(new BigDecimal("0.60"));
        buyNo.setQuantity(50);
        buyNo.setFilledQuantity(0);
        buyNo = orderService.createOrder(buyNo);

        // Need another user to match against
        Order sellYes = new Order();
        sellYes.setUserId(user2.getUuid());
        sellYes.setMarketId(market.getUuid());
        sellYes.setSide(OrderSide.SELL);
        sellYes.setPrice(new BigDecimal("0.60"));
        sellYes.setQuantity(50);
        sellYes.setFilledQuantity(0);
        orderService.createOrder(sellYes);

        Order sellNo = new Order();
        sellNo.setUserId(user2.getUuid());
        sellNo.setMarketId(market.getUuid());
        sellNo.setSide(OrderSide.BUY);
        sellNo.setPrice(new BigDecimal("0.60"));
        sellNo.setQuantity(50);
        sellNo.setFilledQuantity(0);
        orderService.createOrder(sellNo);

        // Check user1's position
        Position position1 = positionRepository
                .findByUserIdAndMarketId(user1.getUuid(), market.getUuid())
                .orElseThrow();

        // Should have equal YES and NO shares (hedged)
        assertEquals(50, position1.getYesShares());
        assertEquals(50, position1.getNoShares());
        assertEquals(0, position1.getNetExposure()); // Perfectly hedged
    }

    @Test
    void testInsufficientFunds() {
        // Try to buy more than wallet balance
        Order bigOrder = new Order();
        bigOrder.setUserId(user1.getUuid());
        bigOrder.setMarketId(market.getUuid());
        bigOrder.setSide(OrderSide.BUY);
        bigOrder.setPrice(new BigDecimal("0.80"));
        bigOrder.setQuantity(2000); // Would need $1600, but only has $1000
        bigOrder.setFilledQuantity(0);

        assertThrows(IllegalStateException.class, () -> {
            orderService.createOrder(bigOrder);
        });
    }

    @Test
    void testOrderCancellation() {
        // Create order
        Order order = new Order();
        order.setUserId(user1.getUuid());
        order.setMarketId(market.getUuid());
        order.setSide(OrderSide.BUY);
        order.setPrice(new BigDecimal("0.60"));
        order.setQuantity(100);
        order.setFilledQuantity(0);
        order = orderService.createOrder(order);

        // Cancel it
        Order cancelled = orderService.cancelOrder(order.getUuid());

        assertEquals(OrderState.CANCELLED, cancelled.getState());

        // Funds should be unlocked
        Wallet wallet = walletService.getWalletByUserId(user1.getUuid());
        assertEquals(0, new BigDecimal("1000.00").compareTo(wallet.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getLockedBalance()));
    }
}
