package usc.uscPredict.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Home controller for the root path.
 * Provides basic API information.
 */
@RestController
@RequestMapping("/api/v1")
public class HomeController {

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
            "documentation", "Import the Postman collection: USC_Predict_Postman_Collection.json"
        );
    }
}
