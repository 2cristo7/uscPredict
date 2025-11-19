package usc.uscPredict.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Event {

    // --- Interfaces para vistas JSON ---
    public interface EventSummaryView {}
    public interface EventDetailView extends EventSummaryView {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonView(EventSummaryView.class)
    private UUID uuid;

    @NotBlank(message = "Title cannot be empty")
    @Column(nullable = false)
    @JsonView(EventSummaryView.class)
    private String title;

    @Column(columnDefinition = "TEXT")
    @JsonView(EventDetailView.class)
    private String description;

    @NotNull(message = "Event state cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonView(EventSummaryView.class)
    private EventState state;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @JsonView(EventSummaryView.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime createdAt;

    public Event() {}

    public Event(String title, String description, EventState state) {
        this.title = title;
        this.description = description;
        this.state = state;
    }
}
