package usc.uscPredict.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Evento de predicción donde los usuarios pueden apostar sobre resultados YES/NO")
@Entity
@Table(name = "events")
@Getter @Setter @NoArgsConstructor
public class Event {

    @Schema(description = "Identificador único del evento", example = "123e4567-e89b-12d3-a456-426614174000", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Schema(description = "Título del evento de predicción", example = "¿Ganará el equipo X el próximo partido?", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Column(name = "title", nullable = false)
    private String title;

    @Schema(description = "Descripción detallada del evento y condiciones de resolución", example = "Este mercado se resolverá basado en el resultado oficial del partido del 15 de octubre", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Schema(description = "Fecha y hora de inicio del evento", example = "2025-10-15T18:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Schema(description = "Fecha y hora de finalización del evento", example = "2025-10-15T21:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Schema(description = "Estado actual del evento", example = "ACTIVE", allowableValues = {"ACTIVE", "CLOSED", "RESOLVED"}, defaultValue = "ACTIVE")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventState state = EventState.ACTIVE;

    @Schema(description = "Usuario que creó el evento", requiredMode = Schema.RequiredMode.REQUIRED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Schema(description = "Precio actual de las acciones YES (entre 0 y 1)", example = "0.65", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(name = "yes_price", nullable = false)
    private Double yesPrice;

    @Schema(description = "Precio actual de las acciones NO (entre 0 y 1)", example = "0.35", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(name = "no_price", nullable = false)
    private Double noPrice;

    @Schema(description = "Lista de UUIDs de órdenes en el order book YES", accessMode = Schema.AccessMode.READ_ONLY)
    @ElementCollection
    @CollectionTable(
            name = "event_yes_order_book",
            joinColumns = @JoinColumn(name = "event_id")
    )
    @Column(name = "order_id")
    private List<UUID> yesOrderBook;

    @Schema(description = "Lista de UUIDs de órdenes en el order book NO", accessMode = Schema.AccessMode.READ_ONLY)
    @ElementCollection
    @CollectionTable(
            name = "event_no_order_book",
            joinColumns = @JoinColumn(name = "event_id")
    )
    @Column(name = "order_id")
    private List<UUID> noOrderBook;

}
