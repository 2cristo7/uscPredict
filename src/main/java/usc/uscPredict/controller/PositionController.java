package usc.uscPredict.controller;

import com.fasterxml.jackson.annotation.JsonView;
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
import usc.uscPredict.model.Position;
import usc.uscPredict.service.PositionService;

import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for Position endpoints.
 * Handles HTTP requests for position tracking and management.
 * Base path: /api/v1/positions
 */
@Tag(name = "Positions", description = "API de seguimiento de posiciones de usuarios")
@RestController
@RequestMapping("/api/v1/positions")
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
    @Operation(
            summary = "Listar todas las posiciones",
            description = "Obtiene una lista completa de todas las posiciones de usuarios en todos los mercados"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de posiciones obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Position.class)))
    })
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
    @Operation(
            summary = "Obtener posición por ID",
            description = "Busca y retorna una posición específica utilizando su identificador UUID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Posición encontrada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Position.class))),
            @ApiResponse(responseCode = "404", description = "Posición no encontrada", content = @Content)
    })
    @GetMapping("/{uuid}")
    @JsonView(Position.PositionDetailView.class)
    public ResponseEntity<Position> getPositionById(
            @Parameter(description = "UUID de la posición a buscar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
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
    @Operation(
            summary = "Buscar posiciones por usuario",
            description = "Retorna todas las posiciones activas de un usuario en todos los mercados"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de posiciones obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Position.class)))
    })
    @GetMapping("/user/{userId}")
    @JsonView(Position.PositionSummaryView.class)
    public ResponseEntity<Set<Position>> getPositionsByUserId(
            @Parameter(description = "UUID del usuario", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
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
    @Operation(
            summary = "Buscar posiciones por mercado",
            description = "Retorna todas las posiciones de todos los usuarios en un mercado específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de posiciones obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Position.class)))
    })
    @GetMapping("/market/{marketId}")
    @JsonView(Position.PositionSummaryView.class)
    public ResponseEntity<Set<Position>> getPositionsByMarketId(
            @Parameter(description = "UUID del mercado", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID marketId) {
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
    @Operation(
            summary = "Obtener posición de usuario en mercado",
            description = "Retorna la posición específica de un usuario en un mercado determinado"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Posición encontrada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Position.class))),
            @ApiResponse(responseCode = "404", description = "Posición no encontrada", content = @Content)
    })
    @GetMapping("/user/{userId}/market/{marketId}")
    @JsonView(Position.PositionDetailView.class)
    public ResponseEntity<Position> getPositionByUserIdAndMarketId(
            @Parameter(description = "UUID del usuario", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,
            @Parameter(description = "UUID del mercado", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
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
    @Operation(
            summary = "Crear posición manualmente",
            description = "Crea una nueva posición de forma manual. NOTA: Las posiciones normalmente se crean/actualizan automáticamente durante el matching de órdenes"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Posición creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Position.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content)
    })
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
    @Operation(
            summary = "Actualizar posición manualmente",
            description = "Actualiza una posición existente de forma manual. NOTA: Las posiciones normalmente se actualizan automáticamente durante el matching de órdenes"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Posición actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Position.class))),
            @ApiResponse(responseCode = "404", description = "Posición no encontrada", content = @Content),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content)
    })
    @PutMapping("/{uuid}")
    @JsonView(Position.PositionDetailView.class)
    public ResponseEntity<Position> updatePosition(
            @Parameter(description = "UUID de la posición a actualizar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
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
    @Operation(
            summary = "Eliminar posición",
            description = "Elimina permanentemente una posición. ADVERTENCIA: Solo para desarrollo/pruebas"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Posición eliminada exitosamente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Posición no encontrada", content = @Content)
    })
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deletePosition(
            @Parameter(description = "UUID de la posición a eliminar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid) {
        boolean deleted = positionService.deletePosition(uuid);
        if (deleted) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
