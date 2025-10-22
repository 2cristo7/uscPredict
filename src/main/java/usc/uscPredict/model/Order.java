package usc.uscPredict.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user_id", columnList = "userId"),
    @Index(name = "idx_order_market_id", columnList = "marketId"),
    @Index(name = "idx_order_state", columnList = "state")
})
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @NotNull(message = "User ID cannot be null")
    @Column(nullable = false)
    private UUID userId;

    @NotNull(message = "Market ID cannot be null")
    @Column(nullable = false)
    private UUID marketId;

    @NotNull(message = "Order side cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @DecimalMax(value = "1.0", inclusive = true, message = "Price must be at most 1")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @Min(value = 0, message = "Filled quantity cannot be negative")
    @Column(nullable = false)
    private Integer filledQuantity = 0;

    @NotNull(message = "Order state cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderState state = OrderState.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Order() {
        this.filledQuantity = 0;
        this.state = OrderState.PENDING;
    }

    public Order(UUID userId, UUID marketId, OrderSide side, BigDecimal price, Integer quantity) {
        this.userId = userId;
        this.marketId = marketId;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.filledQuantity = 0;
        this.state = OrderState.PENDING;
    }
}
