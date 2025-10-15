package usc.uscPredict.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Schema(description = "Request para agregar una orden al order book de un evento")
@Getter
@Setter
@NoArgsConstructor
public class AddOrderRequest {

    @Schema(description = "UUID de la orden a agregar", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Order ID cannot be null")
    private UUID orderId;

    @Schema(description = "Tipo de orden: 'yes' para order book YES, 'no' para order book NO", example = "yes", allowableValues = {"yes", "no"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Order type must be 'yes' or 'no'")
    private String type;

    public AddOrderRequest(UUID orderId, String type) {
        this.orderId = orderId;
        this.type = type;
    }
}
