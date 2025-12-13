package usc.uscPredict.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Order {

    // --- Interfaces para vistas JSON ---
    public interface OrderSummaryView {}
    public interface OrderDetailView extends OrderSummaryView {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonView(OrderSummaryView.class)
    private UUID uuid;

    @NotNull(message = "User ID cannot be null")
    @Column(nullable = false)
    @JsonView(OrderDetailView.class)
    private UUID userId;

    @NotNull(message = "Market ID cannot be null")
    @Column(nullable = false)
    @JsonView(OrderSummaryView.class)
    private UUID marketId;

    @NotNull(message = "Order side cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonView(OrderSummaryView.class)
    private OrderSide side;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @DecimalMax(value = "1.0", inclusive = true, message = "Price must be at most 1")
    @Column(nullable = false, precision = 19, scale = 4)
    @JsonView(OrderSummaryView.class)
    private BigDecimal price;

    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    @JsonView(OrderSummaryView.class)
    private Integer quantity;

    @NotNull
    @Min(value = 0, message = "Filled quantity cannot be negative")
    @Column(nullable = false)
    @JsonView(OrderDetailView.class)
    private Integer filledQuantity = 0;

    @NotNull(message = "Order state cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonView(OrderSummaryView.class)
    private OrderState state = OrderState.PENDING;

    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax(value = "1.0", inclusive = true)
    @Column(precision = 19, scale = 4)
    @JsonView(OrderDetailView.class)
    private BigDecimal executionPrice;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @JsonView(OrderDetailView.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    @JsonView(OrderDetailView.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
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
