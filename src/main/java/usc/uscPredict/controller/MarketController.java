package usc.uscPredict.controller;

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
 * Base path: /markets
 */
@RestController
@RequestMapping("/markets")
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
    public ResponseEntity<Market> getMarketById(@PathVariable UUID uuid) {
        Market market = marketService.getMarketById(uuid);
        if (market != null) {
            return new ResponseEntity<>(market, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * POST /markets
     * Creates a new market.
     * @param market The market data (JSON body)
     * @return 201 CREATED with created market, or 400 BAD REQUEST
     */
    @PostMapping
    public ResponseEntity<Market> createMarket(@RequestBody @Valid @NonNull Market market) {
        Market created = marketService.createMarket(market);
        if (created != null) {
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * PUT /markets/{uuid}
     * Updates an existing market.
     * @param uuid The market UUID
     * @param market The updated market data
     * @return 200 OK with updated market, or 404 NOT FOUND
     */
    @PutMapping("/{uuid}")
    public ResponseEntity<Market> updateMarket(
            @PathVariable UUID uuid,
            @RequestBody @Valid @NonNull Market market) {
        Market updated = marketService.updateMarket(uuid, market);
        if (updated != null) {
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
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
    public ResponseEntity<Market> patchMarket(
            @PathVariable UUID uuid,
            @RequestBody List<Map<String, Object>> updates) {
        try {
            // 1. Obter o mercado da base de datos
            Market existingMarket = marketService.getMarketById(uuid);
            if (existingMarket == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 2. Aplicar as operacións JSON-Patch
            Market patchedMarket = patchUtils.applyPatch(existingMarket, updates);

            // 3. Gardar o recurso actualizado
            Market updated = marketService.updateMarket(uuid, patchedMarket);
            return new ResponseEntity<>(updated, HttpStatus.OK);

        } catch (JsonPatchException e) {
            // Erro ao aplicar o parche (operación inválida, path incorrecto, etc.)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * POST /markets/{uuid}/settle
     * Settles a market (resolves the outcome and distributes winnings).
     * This is called when the event concludes.
     * @param uuid The market UUID
     * @return 200 OK with settled market, or 404 NOT FOUND
     */
    @PostMapping("/{uuid}/settle")
    public ResponseEntity<Market> settleMarket(@PathVariable UUID uuid) {
        Market settled = marketService.settleMarket(uuid);
        if (settled != null) {
            return new ResponseEntity<>(settled, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
