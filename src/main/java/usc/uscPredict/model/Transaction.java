package usc.uscPredict.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transaction {

    // --- Interfaces para vistas JSON ---
    public interface TransactionSummaryView {}
    public interface TransactionDetailView extends TransactionSummaryView {}

    public static Object TransactionType;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonView(TransactionSummaryView.class)
    private UUID uuid;

    @NotNull(message = "User ID cannot be null")
    @Column(nullable = false)
    @JsonView(TransactionDetailView.class)
    private UUID userId;

    @NotNull(message = "Transaction type cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonView(TransactionSummaryView.class)
    private TransactionType type;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 4)
    @JsonView(TransactionSummaryView.class)
    private BigDecimal amount;

    @Column
    @JsonView(TransactionDetailView.class)
    private UUID relatedOrderId;

    @Column(columnDefinition = "TEXT")
    @JsonView(TransactionDetailView.class)
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @JsonView(TransactionSummaryView.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
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
