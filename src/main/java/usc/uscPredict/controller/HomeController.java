package usc.uscPredict.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Home controller for the root path.
 * Provides basic API information.
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
            "message", "USC Predict API",
            "version", "0.1.0",
            "status", "running",
            "endpoints", Map.of(
                "health", "/users/health",
                "events", "/events",
                "markets", "/markets",
                "orders", "/orders",
                "transactions", "/transactions",
                "users", "/users"
            ),
            "documentation", "Import the Postman collection: USC_Predict_Postman_Collection.json"
        );
    }
}
