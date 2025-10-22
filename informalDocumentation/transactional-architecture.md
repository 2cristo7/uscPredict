# Transactional Architecture - Order Creation

## Problem Statement

When implementing order creation for the prediction market, we encountered a critical Spring framework limitation: **@Transactional self-invocation does not work**.

## The Self-Invocation Problem

### What is Self-Invocation?

Self-invocation occurs when a method in a class calls another method in the **same class**:

```java
@Service
public class OrderService {

    public void methodA() {
        this.methodB(); // Self-invocation
    }

    @Transactional
    public void methodB() {
        // This transaction will NOT be created!
    }
}
```

### Why Doesn't @Transactional Work with Self-Invocation?

Spring implements `@Transactional` using **AOP proxies**. Here's how it works:

#### Normal Case (Works ✅):
```
Controller → [Spring Proxy] → @Transactional method → Database
              ↑
              Proxy intercepts call and creates transaction
```

#### Self-Invocation (Broken ❌):
```
methodA() → this.methodB() → Database
            ↑
            Direct call, bypasses proxy, NO transaction!
```

When you call a method using `this.methodB()`, you're calling the **actual object**, not the Spring proxy. The proxy is never invoked, so the transaction is never created.

## Our Initial Problematic Design

```java
@Service
public class OrderService {

    // Public method - orchestrates creation and matching
    public Order createOrder(Order order) {
        Order created = createOrderTransaction(order); // ❌ Self-invocation!
        marketService.matchOrders(order.getMarketId());
        return created;
    }

    // Transactional method - should create transaction
    @Transactional
    private Order createOrderTransaction(Order order) {
        // Validate, lock funds, save order...
        return orderRepository.save(order);
    }
}
```

**Problem**: `createOrderTransaction()` is never executed in a transaction because it's called via self-invocation.

## Solution: OrderPersistenceService

We separated the transactional logic into a **dedicated service**:

### Architecture

```
OrderService (Orchestrator)
    │
    ├─→ OrderPersistenceService.persistOrder() [Transaction 1]
    │   ├─ Validate user/market
    │   ├─ Calculate funds
    │   ├─ Lock funds
    │   ├─ Save order (PENDING)
    │   └─ Create transaction record
    │
    └─→ MarketService.matchOrders() [Transaction 2]
        ├─ Find matching orders
        ├─ Execute trades
        └─ Update positions
```

### Implementation

**OrderPersistenceService.java** (NEW):
```java
@Service
public class OrderPersistenceService {

    @Transactional  // ✅ Works because called from ANOTHER service
    public Order persistOrder(Order order) {
        // Atomic transaction:
        // 1. Validate
        // 2. Lock funds
        // 3. Save order
        // 4. Create audit record
        return orderRepository.save(order);
    }
}
```

**OrderService.java** (MODIFIED):
```java
@Service
public class OrderService {

    @Autowired
    private OrderPersistenceService orderPersistenceService;

    @Autowired
    private MarketService marketService;

    public Order createOrder(Order order) {
        // Phase 1: Persist (cross-service call ✅)
        Order created = orderPersistenceService.persistOrder(order);

        // Phase 2: Match (separate transaction)
        try {
            marketService.matchOrders(order.getMarketId());
        } catch (Exception e) {
            // Order remains PENDING, can be matched later
        }

        return created;
    }
}
```

## Why This Solution Works

### Cross-Service Invocation

```
OrderService.createOrder()
    ↓
    [Spring Proxy for OrderPersistenceService]
    ↓
    OrderPersistenceService.persistOrder() ← Transaction Created! ✅
```

Because the call crosses **service boundaries**, Spring's proxy is invoked, and the transaction is created.

## Transactional Guarantees

### Transaction 1: Order Persistence (Atomic)

**Scope**: Everything in `OrderPersistenceService.persistOrder()`

**Success**:
- Order exists in database (state = PENDING)
- Funds are locked in wallet
- Transaction audit record exists

**Failure**:
- Complete rollback
- No database changes
- Exception propagated to caller

### Transaction 2: Order Matching (Independent)

**Scope**: Everything in `MarketService.matchOrders()`

**Success**:
- Orders matched and filled
- Positions updated
- Funds consumed

**Failure**:
- Order remains PENDING (already persisted in Transaction 1)
- Can be matched later manually
- Exception caught and logged, not propagated

## Key Design Principles

### 1. Separation of Concerns

- **OrderService**: Orchestration (no transactions)
- **OrderPersistenceService**: Transactional persistence
- **MarketService**: Transactional matching

### 2. Two-Phase Commit Pattern

Phase 1 and Phase 2 are **independent transactions**. This prevents:
- Valid orders being lost due to matching failures
- Transactional deadlocks between persistence and matching
- Coupling between order creation and market state

### 3. Fail-Safe Design

If matching fails:
- Order is **not lost** (already persisted)
- User funds are **safely locked** (can be unlocked via cancellation)
- Order can be **matched later** (manually or via retry)

## Alternative Solutions Considered

### ❌ Option 1: Self-Injection
```java
@Autowired
private OrderService self; // Inject proxy of self

public Order createOrder(Order order) {
    Order created = self.createOrderTransaction(order); // Hacky
}
```

**Rejected**:
- Code smell
- Confusing for developers
- Fragile (depends on Spring configuration)
- Difficult to test

### ❌ Option 2: Make Everything @Transactional
```java
@Transactional
public Order createOrder(Order order) {
    // Persist order
    // Match orders (in same transaction)
}
```

**Rejected**:
- If matching fails, order is lost (rollback)
- Tight coupling between persistence and matching
- Long-running transactions (bad for performance)

### ✅ Option 3: Separate Service (CHOSEN)
- Clean separation of concerns
- Proper transactional boundaries
- Easy to understand and maintain
- Testable

## Developer Guidelines

### DO ✅

1. **Keep OrderService as an orchestrator only**
   - No `@Transactional` methods
   - Delegate to specialized services

2. **Use OrderPersistenceService for all order persistence**
   - Never bypass this service
   - Never move logic back to OrderService

3. **Handle matching failures gracefully**
   - Log errors
   - Inform users via warnings
   - Provide manual matching option

### DON'T ❌

1. **Never add @Transactional to OrderService methods that call other services**
   - Creates nested transactions (complex and error-prone)

2. **Never move persistOrder() back into OrderService**
   - Breaks the @Transactional proxy pattern

3. **Never throw exceptions from createOrder() after persistence succeeds**
   - Order is already created, exception would be misleading

## Testing Implications

### Unit Testing OrderPersistenceService

```java
@Test
void testPersistOrder_InsufficientFunds() {
    // Given: User with low balance
    // When: persistOrder() called
    // Then: IllegalStateException thrown, NO database changes
}
```

### Integration Testing OrderService

```java
@Test
void testCreateOrder_MatchingFails() {
    // Given: Matching will fail
    // When: createOrder() called
    // Then: Order exists as PENDING, no exception thrown
}
```

## Performance Considerations

### Transaction Duration

- **Transaction 1**: Very short (< 100ms typically)
  - Simple validations
  - Single DB write

- **Transaction 2**: Variable (depends on order book size)
  - Iterates through matching orders
  - Multiple DB updates

By separating transactions, we ensure:
- Short locks on wallet table
- No blocking between order creation and matching
- Better concurrency

## Monitoring and Debugging

### Logs to Watch

```
INFO: Order created successfully: uuid=abc123
WARNING: Order matching failed for order abc123. Order remains PENDING. Error: ...
```

### Database State

After `createOrder()` completes:

**Success (matching worked)**:
- Order state: FILLED or PARTIALLY_FILLED
- Wallet locked balance: Reduced or zero
- Positions updated

**Partial Success (matching failed)**:
- Order state: PENDING
- Wallet locked balance: Full amount locked
- Positions unchanged
- Order can be matched via manual endpoint

## Related Documentation

- `OrderService.java` - See class-level and method-level comments
- `OrderPersistenceService.java` - See extensive documentation on transaction scope
- `MarketService.java` - See `matchOrders()` documentation

## References

- [Spring @Transactional Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
- [Spring AOP Proxies](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop-proxying)
- [Self-Invocation Problem](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction-declarative-annotations-method-visibility)
