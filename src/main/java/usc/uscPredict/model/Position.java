package usc.uscPredict.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "positions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "marketId"}),
    indexes = {
        @Index(name = "idx_position_user_id", columnList = "userId"),
        @Index(name = "idx_position_market_id", columnList = "marketId")
    })
@Getter
@Setter
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @NotNull(message = "User ID cannot be null")
    @Column(nullable = false)
    private UUID userId;

    @NotNull(message = "Market ID cannot be null")
    @Column(nullable = false)
    private UUID marketId;

    @NotNull
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal avgPrice;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal realizedPnl;

    @Column(precision = 19, scale = 4)
    private BigDecimal unrealizedPnl;

    public Position() {
        this.quantity = 0;
        this.avgPrice = BigDecimal.ZERO;
        this.realizedPnl = BigDecimal.ZERO;
        this.unrealizedPnl = BigDecimal.ZERO;
    }

    public Position(UUID userId, UUID marketId, Integer quantity, BigDecimal avgPrice) {
        this.userId = userId;
        this.marketId = marketId;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.realizedPnl = BigDecimal.ZERO;
        this.unrealizedPnl = BigDecimal.ZERO;
    }
}
