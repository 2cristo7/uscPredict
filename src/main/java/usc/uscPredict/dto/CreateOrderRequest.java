package usc.uscPredict.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import usc.uscPredict.model.OrderType;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request para crear una nueva orden en un evento")
@Getter
@Setter
@NoArgsConstructor
public class CreateOrderRequest {

    @Schema(description = "UUID del usuario que crea la orden", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "User ID cannot be null")
    private UUID userId;

    @Schema(description = "Tipo de orden: BUY para comprar, SELL para vender", example = "BUY", allowableValues = {"BUY", "SELL"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Order type cannot be null")
    private OrderType type;

    @Schema(description = "Precio de la orden (entre 0 y 1 para mercados de predicción)", example = "0.65", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @Schema(description = "Cantidad de acciones a comprar/vender", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be positive")
    private int quantity;

    public CreateOrderRequest(UUID userId, OrderType type, BigDecimal price, int quantity) {
        this.userId = userId;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
    }
}
