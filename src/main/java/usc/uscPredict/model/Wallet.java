package usc.uscPredict.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallets", indexes = {
    @Index(name = "idx_wallet_user_id", columnList = "userId", unique = true)
})
@Getter
@Setter
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @NotNull(message = "User ID cannot be null")
    @Column(nullable = false, unique = true)
    private UUID userId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance must be non-negative")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Locked balance must be non-negative")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal lockedBalance;

    public Wallet() {
        this.balance = BigDecimal.ZERO;
        this.lockedBalance = BigDecimal.ZERO;
    }

    public Wallet(UUID userId) {
        this.userId = userId;
        this.balance = BigDecimal.ZERO;
        this.lockedBalance = BigDecimal.ZERO;
    }

    public Wallet(UUID userId, BigDecimal balance, BigDecimal lockedBalance) {
        this.userId = userId;
        this.balance = balance;
        this.lockedBalance = lockedBalance;
    }
}
