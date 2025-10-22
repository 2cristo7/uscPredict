# USC Predict Project Context

## 1. Overview

**USC Predict** is a prediction market platform inspired by the structure of **Polymarket**, but implemented entirely with traditional backend technologies (no blockchain components). The system allows users to deposit virtual funds, browse prediction events, and trade outcome tokens representing the probability of future events. The core concept is to enable a dynamic market where users can buy or sell exposure to different outcomes, with prices reflecting collective beliefs.

The implementation is built with **Spring Boot**, **JPA/Hibernate**, and **PostgreSQL (Supabase)**. The platform exposes a REST API that manages users, wallets, events, orders, positions, and transactions.

The system’s long-term goal is to simulate a decentralized prediction market environment using centralized architecture — focusing on transparency, auditability, and correctness of market state transitions.

---

## 2. System Architecture

USC Predict follows a clean layered architecture typical of Spring Boot projects:

### **Controller Layer**

* Defines REST endpoints for all entities (users, events, markets, orders, etc.).
* Handles HTTP requests and responses.
* Delegates business logic to the corresponding service layer.
* Uses `ResponseEntity` to control HTTP status and error handling.

### **Service Layer**

* Contains the business logic for each domain.
* Coordinates repositories and domain rules (e.g., validating sufficient funds, updating wallet balances, managing order state changes).
* Encapsulates transactional operations using Spring’s `@Transactional`.

### **Repository Layer**

* Implements persistence via Spring Data JPA repositories.
* Responsible only for database interactions — simple CRUD operations or custom queries.

### **Database Layer**

* Uses PostgreSQL for relational persistence.
* Entity classes are annotated with JPA annotations.
* Timestamps, enums, and relationships are mapped to database fields following naming conventions.

---

## 3. Domain Model

### **User**

Represents a registered participant of the platform.

* Attributes: `uuid`, `name`, `email`, `pswd_hash`, `role`, `created_at`.
* Relationships: owns one `Wallet` and can place many `Orders`.

### **Wallet**

Represents a user’s available funds.

* Attributes: `uuid`, `balance`, `locked_balance`.
* `locked_balance` represents funds temporarily held by open orders.
* Each wallet belongs to one user.

### **Event**

Represents a real-world or virtual event with possible outcomes.

* Attributes: `uuid`, `title`, `description`, `state`, `created_at`.
* `state` is an enum with possible values such as `OPEN`, `SETTLED`, `CLOSED`.
* Each event contains one or more **Markets**.

### **Market**

Represents a tradeable market associated with a specific event outcome.

* Attributes: `uuid`, `event_id`, `outcome`, `status`.
* Each market can have multiple open orders and resulting positions.

### **Order**

Represents an intent to buy or sell shares in a market at a given price.

* Attributes: `uuid`, `user_id`, `market_id`, `side (BUY/SELL)`, `price`, `quantity`, `state (PENDING, FILLED, CANCELLED)`.
* Orders affect wallet balances:

    * On creation: required funds are locked.
    * On fill: funds are transferred and position updates occur.
* Orders can be partially filled or cancelled.

### **Position**

Tracks a user’s current exposure in a given market.

* Attributes: `uuid`, `user_id`, `market_id`, `quantity`, `avg_price`, `realized_pnl`, `unrealized_pnl`.
* Updated whenever orders are executed.

### **Transaction**

Represents any change in a user’s balance or position.

* Attributes: `uuid`, `user_id`, `type`, `amount`, `created_at`.
* Types include: `DEPOSIT`, `WITHDRAWAL`, `ORDER_PLACED`, `ORDER_EXECUTED`, `ORDER_CANCELLED`.

---

## 4. Typical Market Flow

1. **User Authentication**

    * The user logs in or registers through the REST API.
    * A `Wallet` is created and initialized with zero or demo balance.

2. **Event Discovery**

    * The API returns a list of available events and markets.
    * Each market corresponds to a binary or categorical outcome.

3. **Placing an Order**

    * The user creates a `BUY` or `SELL` order for a market.
    * The service validates that the user’s wallet has sufficient available funds.
    * The order amount is locked in the wallet and the order is persisted with state `PENDING`.

4. **Order Matching (to be implemented)**

    * When another order with compatible price and side exists, a match occurs.
    * The system updates both orders’ filled quantities and creates a corresponding `Transaction`.
    * **Pricing and matching logic will be custom-designed** — this part is intentionally left open.

5. **Position Update**

    * After a trade, the system updates or creates a `Position` for each involved user.
    * Positions aggregate the user’s exposure and can be used to calculate realized/unrealized PnL.

6. **Event Resolution**

    * When the event concludes, the admin sets the final outcome.
    * Markets are settled; user balances and positions are updated accordingly.

---

## 5. Market Logic (Expected Behavior)

The core market logic is intentionally left open for future design. The expected components are:

* **Order Book Management:** maintain a list of open BUY and SELL orders per market.
* **Matching Engine:** find compatible orders and execute trades when price and side conditions are met.
* **Wallet Management:** handle balance updates, locking/unlocking, and transaction creation.
* **Position Management:** aggregate executed trades into per-user positions.
* **PnL Calculation:** compute realized and unrealized profit/loss at any time (optional for later stages).

This logic will eventually be handled in the service layer under transactional boundaries.

---

## 6. Current Development State

* User entity, controller, and basic CRUD operations are implemented.
* Commenting feature is complete (for event discussion).
* Event entity design is defined and ready for implementation.
* Wallet and transaction mechanisms are planned but partially stubbed.
* Order and position logic will be implemented next.
* No pricing or matching engine exists yet — this is the next major milestone.

---

## 7. Development Goals for Claude Code

Claude should use this document to:

* Understand the system’s architecture and purpose.
* Maintain consistency with Spring Boot service patterns.
* Implement or refactor features respecting the transactional market flow.
* Avoid designing pricing or order-matching algorithms — those will be defined manually later.
* Focus on structural correctness, domain integrity, and RESTful API consistency.

---

**Summary:**
USC Predict is a Spring Boot–based backend simulating a prediction market platform similar to Polymarket, minus blockchain infrastructure. It manages users, wallets, orders, markets, and positions. The upcoming development tasks involve implementing market logic (order management, balance locking, transaction recording, position tracking) in a clean and extensible way.
