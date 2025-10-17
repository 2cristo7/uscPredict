package usc.uscPredict.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_user_id", columnList = "userId"),
    @Index(name = "idx_transaction_created_at", columnList = "createdAt")
})
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @NotNull(message = "User ID cannot be null")
    @Column(nullable = false)
    private UUID userId;

    @NotNull(message = "Transaction type cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column
    private UUID relatedOrderId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Transaction() {}

    public Transaction(UUID userId, TransactionType type, BigDecimal amount) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
    }

    public Transaction(UUID userId, TransactionType type, BigDecimal amount, UUID relatedOrderId, String description) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.relatedOrderId = relatedOrderId;
        this.description = description;
    }
}
