package usc.uscPredict.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.fge.jsonpatch.JsonPatchException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Markets", description = "API de gestión de mercados de predicción")
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
    @Operation(
            summary = "Listar todos los mercados",
            description = "Obtiene una lista completa de todos los mercados de predicción disponibles en la plataforma"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de mercados obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Market.class)))
    })
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
    @Operation(
            summary = "Obtener mercado por ID",
            description = "Busca y retorna un mercado específico utilizando su identificador UUID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mercado encontrado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Market.class))),
            @ApiResponse(responseCode = "404", description = "Mercado no encontrado", content = @Content)
    })
    @GetMapping("/{uuid}")
    @JsonView(Market.MarketDetailView.class)
    public ResponseEntity<Market> getMarketById(
            @Parameter(description = "UUID del mercado a buscar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
        Market market = marketService.getMarketById(uuid);
        return ResponseEntity.ok(market);
    }

    /**
     * POST /markets
     * Creates a new market.
     * @param market The market data (JSON body)
     * @return 201 CREATED with created market, or 400 BAD REQUEST
     */
    @Operation(
            summary = "Crear nuevo mercado",
            description = "Crea un nuevo mercado de predicción con los datos proporcionados"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Mercado creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Market.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content)
    })
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
    @Operation(
            summary = "Actualizar mercado completo",
            description = "Actualiza todos los campos de un mercado existente con los nuevos datos proporcionados"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mercado actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Market.class))),
            @ApiResponse(responseCode = "404", description = "Mercado no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content)
    })
    @PutMapping("/{uuid}")
    @JsonView(Market.MarketDetailView.class)
    public ResponseEntity<Market> updateMarket(
            @Parameter(description = "UUID del mercado a actualizar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
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
    @Operation(
            summary = "Eliminar mercado",
            description = "Elimina permanentemente un mercado del sistema"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Mercado eliminado exitosamente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mercado no encontrado", content = @Content)
    })
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteMarket(
            @Parameter(description = "UUID del mercado a eliminar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
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
    @Operation(
            summary = "Buscar mercados por evento",
            description = "Retorna todos los mercados asociados a un evento específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de mercados obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Market.class)))
    })
    @GetMapping("/event/{eventId}")
    @JsonView(Market.MarketSummaryView.class)
    public ResponseEntity<Set<Market>> getMarketsByEventId(
            @Parameter(description = "UUID del evento", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID eventId) {
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
    @Operation(
            summary = "Buscar mercados por estado",
            description = "Retorna todos los mercados que tienen un estado específico (ACTIVE, SUSPENDED, SETTLED)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de mercados obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Market.class)))
    })
    @GetMapping("/status/{status}")
    @JsonView(Market.MarketSummaryView.class)
    public ResponseEntity<Set<Market>> getMarketsByStatus(
            @Parameter(description = "Estado del mercado", required = true, example = "ACTIVE")
            @PathVariable MarketStatus status) {
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
    @Operation(
            summary = "Actualizar mercado parcialmente con JSON-Patch",
            description = "Aplica operaciones JSON-Patch (RFC 6902) para modificar campos específicos de un mercado sin reemplazar toda la entidad"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mercado actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Market.class))),
            @ApiResponse(responseCode = "404", description = "Mercado no encontrado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Operación JSON-Patch inválida", content = @Content)
    })
    @PatchMapping("/{uuid}")
    @JsonView(Market.MarketDetailView.class)
    public ResponseEntity<Market> patchMarket(
            @Parameter(description = "UUID del mercado a actualizar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
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
    @Operation(
            summary = "Resolver mercado",
            description = "Resuelve el resultado final de un mercado y distribuye las ganancias a los ganadores. Esta acción se ejecuta cuando el evento concluye"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mercado resuelto exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Market.class))),
            @ApiResponse(responseCode = "404", description = "Mercado no encontrado", content = @Content)
    })
    @PostMapping("/{uuid}/settle")
    @JsonView(Market.MarketDetailView.class)
    public ResponseEntity<Market> settleMarket(
            @Parameter(description = "UUID del mercado a resolver", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
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
    @Operation(
            summary = "Ejecutar motor de matching",
            description = "Activa el motor de matching para emparejar órdenes de compra y venta pendientes en el mercado"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Motor de matching ejecutado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchResult.class)))
    })
    @PostMapping("/{uuid}/match")
    public ResponseEntity<MatchResult> matchOrders(
            @Parameter(description = "UUID del mercado", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
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
