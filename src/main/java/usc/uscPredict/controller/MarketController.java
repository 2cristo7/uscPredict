package usc.uscPredict.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.fge.jsonpatch.JsonPatchException;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.model.Market;
import usc.uscPredict.model.MarketStatus;
import usc.uscPredict.service.MarketService;
import usc.uscPredict.util.PatchUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for Market endpoints.
 * Handles HTTP requests for market management.
 * Base path: /api/v1/markets
 */
@RestController
@RequestMapping("/api/v1/markets")
@Validated
public class MarketController {

    private final MarketService marketService;
    private final PatchUtils patchUtils;

    @Autowired
    public MarketController(MarketService marketService, PatchUtils patchUtils) {
        this.marketService = marketService;
        this.patchUtils = patchUtils;
    }

    /**
     * GET /markets
     * Retrieves all markets.
     * @return 200 OK with list of markets
     */
    @GetMapping
    @JsonView(Market.MarketSummaryView.class)
    public ResponseEntity<Set<Market>> getAllMarkets() {
        Set<Market> markets = marketService.getAllMarkets();
        return new ResponseEntity<>(markets, HttpStatus.OK);
    }

    /**
     * GET /markets/{uuid}
     * Retrieves a single market by UUID.
     * @param uuid The market UUID
     * @return 200 OK with market, or 404 NOT FOUND
     */
    @GetMapping("/{uuid}")
    @JsonView(Market.MarketDetailView.class)
    public ResponseEntity<Market> getMarketById(@PathVariable UUID uuid) {
        Market market = marketService.getMarketById(uuid);
        return ResponseEntity.ok(market);
    }

    /**
     * POST /markets
     * Creates a new market.
     * @param market The market data (JSON body)
     * @return 201 CREATED with created market, or 400 BAD REQUEST
     */
    @PostMapping
    @JsonView(Market.MarketDetailView.class)
    public ResponseEntity<Market> createMarket(@RequestBody @Valid @NonNull Market market) {
        Market created = marketService.createMarket(market);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * PUT /markets/{uuid}
     * Updates an existing market.
     * @param uuid The market UUID
     * @param market The updated market data
     * @return 200 OK with updated market, or 404 NOT FOUND
     */
    @PutMapping("/{uuid}")
    @JsonView(Market.MarketDetailView.class)
    public ResponseEntity<Market> updateMarket(
            @PathVariable UUID uuid,
            @RequestBody @Valid @NonNull Market market) {
        Market updated = marketService.updateMarket(uuid, market);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /markets/{uuid}
     * Deletes a market.
     * @param uuid The market UUID
     * @return 204 NO CONTENT if deleted, or 404 NOT FOUND
     */
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteMarket(@PathVariable UUID uuid) {
        boolean deleted = marketService.deleteMarket(uuid);
        if (deleted) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * GET /markets/event/{eventId}
     * Retrieves all markets for a specific event.
     * Example: GET /markets/event/123e4567-e89b-12d3-a456-426614174000
     * @param eventId The event UUID
     * @return 200 OK with list of markets
     */
    @GetMapping("/event/{eventId}")
    @JsonView(Market.MarketSummaryView.class)
    public ResponseEntity<Set<Market>> getMarketsByEventId(@PathVariable UUID eventId) {
        Set<Market> markets = marketService.getMarketsByEventId(eventId);
        return new ResponseEntity<>(markets, HttpStatus.OK);
    }

    /**
     * GET /markets/status/{status}
     * Retrieves all markets with a specific status.
     * Example: GET /markets/status/ACTIVE
     * @param status The market status (ACTIVE, SUSPENDED, SETTLED)
     * @return 200 OK with list of markets
     */
    @GetMapping("/status/{status}")
    @JsonView(Market.MarketSummaryView.class)
    public ResponseEntity<Set<Market>> getMarketsByStatus(@PathVariable MarketStatus status) {
        Set<Market> markets = marketService.getMarketsByStatus(status);
        return new ResponseEntity<>(markets, HttpStatus.OK);
    }

    /**
     * PATCH /markets/{uuid}
     * Applies JSON-Patch operations to a market (RFC 6902).
     * Body: [{ "op": "replace", "path": "/status", "value": "SUSPENDED" }]
     * @param uuid The market UUID
     * @param updates List of JSON-Patch operations
     * @return 200 OK with updated market, 404 NOT FOUND, or 400 BAD REQUEST
     */
    @PatchMapping("/{uuid}")
    @JsonView(Market.MarketDetailView.class)
    public ResponseEntity<Market> patchMarket(
            @PathVariable UUID uuid,
            @RequestBody List<Map<String, Object>> updates) throws JsonPatchException {
        // 1. Obter o mercado da base de datos (throws exception if not found)
        Market existingMarket = marketService.getMarketById(uuid);

        // 2. Aplicar as operacións JSON-Patch (JsonPatchException handled globally)
        Market patchedMarket = patchUtils.applyPatch(existingMarket, updates);

        // 3. Gardar o recurso actualizado
        Market updated = marketService.updateMarket(uuid, patchedMarket);
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /markets/{uuid}/settle
     * Settles a market (resolves the outcome and distributes winnings).
     * This is called when the event concludes.
     * @param uuid The market UUID
     * @param request Body containing winningOutcome (YES or NO)
     * @return 200 OK with settled market, or 404 NOT FOUND
     */
    @PostMapping("/{uuid}/settle")
    @JsonView(Market.MarketDetailView.class)
    public ResponseEntity<Market> settleMarket(
            @PathVariable UUID uuid,
            @RequestBody SettleRequest request) {
        Market settled = marketService.settleMarket(uuid, request.getWinningOutcome());
        return ResponseEntity.ok(settled);
    }

    /**
     * Request body for settle endpoint.
     */
    public static class SettleRequest {
        private String winningOutcome;

        public String getWinningOutcome() {
            return winningOutcome;
        }

        public void setWinningOutcome(String winningOutcome) {
            this.winningOutcome = winningOutcome;
        }
    }

    /**
     * POST /markets/{uuid}/match
     * Triggers the matching engine for a specific market.
     * This attempts to match pending buy and sell orders.
     * @param uuid The market UUID
     * @return 200 OK with number of matches executed
     */
    @PostMapping("/{uuid}/match")
    public ResponseEntity<MatchResult> matchOrders(@PathVariable UUID uuid) {
        int matchCount = marketService.matchOrders(uuid);
        MatchResult result = new MatchResult(matchCount);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * GET /markets/{uuid}/price-history
     * Retrieves price history for a market.
     * @param uuid The market UUID
     * @param bucketMinutes Optional bucket size in minutes (default: 60)
     * @return 200 OK with list of price points
     */
    @GetMapping("/{uuid}/price-history")
    public ResponseEntity<List<MarketService.PricePoint>> getPriceHistory(
            @PathVariable UUID uuid,
            @RequestParam(defaultValue = "60") int bucketMinutes) {
        List<MarketService.PricePoint> history = marketService.getPriceHistory(uuid, bucketMinutes);
        return new ResponseEntity<>(history, HttpStatus.OK);
    }

    /**
     * GET /markets/{uuid}/volume
     * Retrieves total trading volume for a market.
     * @param uuid The market UUID
     * @return 200 OK with volume
     */
    @GetMapping("/{uuid}/volume")
    public ResponseEntity<VolumeResult> getMarketVolume(@PathVariable UUID uuid) {
        java.math.BigDecimal volume = marketService.getMarketVolume(uuid);
        return new ResponseEntity<>(new VolumeResult(volume), HttpStatus.OK);
    }

    /**
     * Inner class for volume result response.
     */
    public static class VolumeResult {
        private java.math.BigDecimal volume;

        public VolumeResult(java.math.BigDecimal volume) {
            this.volume = volume;
        }

        public java.math.BigDecimal getVolume() {
            return volume;
        }

        public void setVolume(java.math.BigDecimal volume) {
            this.volume = volume;
        }
    }

    /**
     * Inner class for match result response.
     */
    public static class MatchResult {
        private int matchesExecuted;

        public MatchResult(int matchesExecuted) {
            this.matchesExecuted = matchesExecuted;
        }

        public int getMatchesExecuted() {
            return matchesExecuted;
        }

        public void setMatchesExecuted(int matchesExecuted) {
            this.matchesExecuted = matchesExecuted;
        }
    }
}
