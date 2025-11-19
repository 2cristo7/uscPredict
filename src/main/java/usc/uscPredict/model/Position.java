package usc.uscPredict.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Position entity represents a user's holdings in a specific prediction market.
 *
 * Key concepts:
 * - yesShares = Number of YES outcome shares owned
 * - noShares = Number of NO outcome shares owned
 * - Both are LONG positions (user paid for them upfront)
 * - At settlement: YES shares pay $1 if event occurs, $0 otherwise
 *                  NO shares pay $1 if event doesn't occur, $0 otherwise
 *
 * Example:
 * - yesShares = 100, noShares = 0 → User bet on YES
 * - yesShares = 0, noShares = 50 → User bet on NO
 * - yesShares = 100, noShares = 50 → User hedged (owns both outcomes)
 */
@Entity
@Table(name = "positions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "marketId"}),
    indexes = {
        @Index(name = "idx_position_user_id", columnList = "userId"),
        @Index(name = "idx_position_market_id", columnList = "marketId")
    })
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Position {

    // --- Interfaces para vistas JSON ---
    public interface PositionSummaryView {}
    public interface PositionDetailView extends PositionSummaryView {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonView(PositionSummaryView.class)
    private UUID uuid;

    @NotNull(message = "User ID cannot be null")
    @Column(nullable = false)
    @JsonView(PositionDetailView.class)
    private UUID userId;

    @NotNull(message = "Market ID cannot be null")
    @Column(nullable = false)
    @JsonView(PositionSummaryView.class)
    private UUID marketId;

    /**
     * Number of YES outcome shares owned by the user.
     * These pay $1 each if the event happens.
     */
    @NotNull
    @Min(value = 0, message = "YES shares cannot be negative")
    @Column(nullable = false)
    @JsonView(PositionSummaryView.class)
    private Integer yesShares = 0;

    /**
     * Number of NO outcome shares owned by the user.
     * These pay $1 each if the event does NOT happen.
     */
    @NotNull
    @Min(value = 0, message = "NO shares cannot be negative")
    @Column(nullable = false)
    @JsonView(PositionSummaryView.class)
    private Integer noShares = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @JsonView(PositionDetailView.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    @JsonView(PositionDetailView.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * No-arg constructor required by JPA.
     */
    public Position() {
        this.yesShares = 0;
        this.noShares = 0;
    }

    /**
     * Constructor for creating a new position.
     * @param userId The user UUID
     * @param marketId The market UUID
     */
    public Position(UUID userId, UUID marketId) {
        this.userId = userId;
        this.marketId = marketId;
        this.yesShares = 0;
        this.noShares = 0;
    }

    /**
     * Checks if user has any YES shares.
     * @return true if yesShares > 0
     */
    public boolean hasYesShares() {
        return yesShares > 0;
    }

    /**
     * Checks if user has any NO shares.
     * @return true if noShares > 0
     */
    public boolean hasNoShares() {
        return noShares > 0;
    }

    /**
     * Gets total shares owned (YES + NO).
     * @return total number of shares
     */
    public int getTotalShares() {
        return yesShares + noShares;
    }

    /**
     * Gets net exposure (YES - NO).
     * Positive = betting on YES, Negative = betting on NO, Zero = hedged
     * @return net position
     */
    public int getNetExposure() {
        return yesShares - noShares;
    }
}
