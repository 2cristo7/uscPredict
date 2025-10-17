package usc.uscPredict.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "markets", indexes = {
    @Index(name = "idx_market_event_id", columnList = "eventId")
})
@Getter
@Setter
public class Market {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @NotNull(message = "Event ID cannot be null")
    @Column(nullable = false)
    private UUID eventId;

    @NotBlank(message = "Outcome cannot be empty")
    @Column(nullable = false)
    private String outcome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketStatus status;

    public Market() {}

    public Market(UUID eventId, String outcome, MarketStatus status) {
        this.eventId = eventId;
        this.outcome = outcome;
        this.status = status;
    }
}
