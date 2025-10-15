package usc.uscPredict.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Orden de compra/venta en un evento de predicción")
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Schema(description = "Identificador único de la orden", example = "123e4567-e89b-12d3-a456-426614174000", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Schema(description = "Evento asociado a esta orden", requiredMode = Schema.RequiredMode.REQUIRED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    @NotNull
    private Event event;

    @Schema(description = "Usuario que creó la orden", requiredMode = Schema.RequiredMode.REQUIRED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @Schema(description = "Tipo de orden", example = "BUY", allowableValues = {"BUY", "SELL"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    @NotNull
    private OrderType type;

    @Schema(description = "Precio de la orden (entre 0 y 1 para mercados de predicción)", example = "0.65", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull
    @Positive
    private BigDecimal price;

    @Schema(description = "Cantidad de acciones en la orden", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(nullable = false)
    @NotNull
    @Positive
    private int quantity;

    @Schema(description = "Estado de la orden", example = "PENDING", allowableValues = {"PENDING", "EXECUTED", "CANCELLED"}, defaultValue = "PENDING")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @NotNull
    private OrderState state = OrderState.PENDING;

    @Schema(description = "Fecha y hora de creación de la orden", example = "2025-10-15T14:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructor con parámetros
    public Order(Event event, User user, OrderType type, BigDecimal price, int quantity) {
        this.event = event;
        this.user = user;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.state = OrderState.PENDING;
    }
}
