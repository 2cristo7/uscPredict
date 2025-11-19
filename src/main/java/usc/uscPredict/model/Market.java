package usc.uscPredict.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Market {

    // --- Interfaces para vistas JSON ---
    public interface MarketSummaryView {}
    public interface MarketDetailView extends MarketSummaryView {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonView(MarketSummaryView.class)
    private UUID uuid;

    @NotNull(message = "Event ID cannot be null")
    @Column(nullable = false)
    @JsonView(MarketSummaryView.class)
    private UUID eventId;

    @NotBlank(message = "Outcome cannot be empty")
    @Column(nullable = false)
    @JsonView(MarketSummaryView.class)
    private String outcome;

    @NotNull(message = "Market status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonView(MarketSummaryView.class)
    private MarketStatus status;

    public Market() {}

    public Market(UUID eventId, String outcome, MarketStatus status) {
        this.eventId = eventId;
        this.outcome = outcome;
        this.status = status;
    }
}
