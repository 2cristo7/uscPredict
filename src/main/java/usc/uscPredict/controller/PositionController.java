package usc.uscPredict.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.model.Position;
import usc.uscPredict.service.PositionService;

import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for Position endpoints.
 * Handles HTTP requests for position tracking and management.
 * Base path: /positions
 */
@RestController
@RequestMapping("/positions")
@Validated
public class PositionController {

    private final PositionService positionService;

    @Autowired
    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    /**
     * GET /positions
     * Retrieves all positions.
     * @return 200 OK with list of positions
     */
    @GetMapping
    @JsonView(Position.PositionSummaryView.class)
    public ResponseEntity<Set<Position>> getAllPositions() {
        Set<Position> positions = positionService.getAllPositions();
        return new ResponseEntity<>(positions, HttpStatus.OK);
    }

    /**
     * GET /positions/{uuid}
     * Retrieves a single position by UUID.
     * @param uuid The position UUID
     * @return 200 OK with position, or 404 NOT FOUND
     */
    @GetMapping("/{uuid}")
    @JsonView(Position.PositionDetailView.class)
    public ResponseEntity<Position> getPositionById(@PathVariable UUID uuid) {
        Position position = positionService.getPositionById(uuid);
        return ResponseEntity.ok(position);
    }

    /**
     * GET /positions/user/{userId}
     * Retrieves all positions for a specific user.
     * Example: GET /positions/user/123e4567-e89b-12d3-a456-426614174000
     * @param userId The user UUID
     * @return 200 OK with list of positions
     */
    @GetMapping("/user/{userId}")
    @JsonView(Position.PositionSummaryView.class)
    public ResponseEntity<Set<Position>> getPositionsByUserId(@PathVariable UUID userId) {
        Set<Position> positions = positionService.getPositionsByUserId(userId);
        return new ResponseEntity<>(positions, HttpStatus.OK);
    }

    /**
     * GET /positions/market/{marketId}
     * Retrieves all positions for a specific market.
     * Example: GET /positions/market/123e4567-e89b-12d3-a456-426614174000
     * @param marketId The market UUID
     * @return 200 OK with list of positions
     */
    @GetMapping("/market/{marketId}")
    @JsonView(Position.PositionSummaryView.class)
    public ResponseEntity<Set<Position>> getPositionsByMarketId(@PathVariable UUID marketId) {
        Set<Position> positions = positionService.getPositionsByMarketId(marketId);
        return new ResponseEntity<>(positions, HttpStatus.OK);
    }

    /**
     * GET /positions/user/{userId}/market/{marketId}
     * Retrieves a position for a specific user and market.
     * Example: GET /positions/user/123e.../market/456e...
     * @param userId The user UUID
     * @param marketId The market UUID
     * @return 200 OK with position, or 404 NOT FOUND
     */
    @GetMapping("/user/{userId}/market/{marketId}")
    @JsonView(Position.PositionDetailView.class)
    public ResponseEntity<Position> getPositionByUserIdAndMarketId(
            @PathVariable UUID userId,
            @PathVariable UUID marketId) {
        Position position = positionService.getPositionByUserIdAndMarketId(userId, marketId);
        return ResponseEntity.ok(position);
    }

    /**
     * POST /positions
     * Creates a new position manually.
     * Note: Positions are usually created/updated automatically during order matching.
     * @param position The position data (JSON body)
     * @return 201 CREATED with created position
     */
    @PostMapping
    @JsonView(Position.PositionDetailView.class)
    public ResponseEntity<Position> createPosition(@RequestBody @Valid @NonNull Position position) {
        Position created = positionService.createPosition(position);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * PUT /positions/{uuid}
     * Updates an existing position.
     * Note: Positions are usually updated automatically during order matching.
     * @param uuid The position UUID
     * @param position The updated position data
     * @return 200 OK with updated position, or 404 NOT FOUND
     */
    @PutMapping("/{uuid}")
    @JsonView(Position.PositionDetailView.class)
    public ResponseEntity<Position> updatePosition(
            @PathVariable UUID uuid,
            @RequestBody @Valid @NonNull Position position) {
        Position updated = positionService.updatePosition(uuid, position);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /positions/{uuid}
     * Deletes a position.
     * WARNING: Only for testing/development.
     * @param uuid The position UUID
     * @return 204 NO CONTENT if deleted, or 404 NOT FOUND
     */
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deletePosition(@PathVariable UUID uuid) {
        boolean deleted = positionService.deletePosition(uuid);
        if (deleted) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
