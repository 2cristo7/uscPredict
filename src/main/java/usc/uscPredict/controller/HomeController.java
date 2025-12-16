package usc.uscPredict.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Home controller for the root path.
 * Provides basic API information.
 */
@Tag(name = "Home", description = "API de información general")
@RestController
@RequestMapping("/api/v1")
public class HomeController {

    @Operation(
            summary = "Información de la API",
            description = "Retorna información general sobre la API USC Predict, incluyendo versión, " +
                    "estado del servicio y rutas disponibles de los endpoints principales"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Información de la API obtenida exitosamente",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
            "message", "USC Predict API",
            "version", "0.1.0",
            "status", "running",
            "endpoints", Map.of(
                "health", "/api/v1/users/health",
                "auth", "/api/v1/auth",
                "users", "/api/v1/users",
                "events", "/api/v1/events",
                "markets", "/api/v1/markets",
                "orders", "/api/v1/orders",
                "positions", "/api/v1/positions",
                "transactions", "/api/v1/transactions",
                "wallets", "/api/v1/wallets",
                "comments", "/api/v1/comments"
            ),
            "documentation", "http://localhost:8080/scalar"
        );
    }
}
