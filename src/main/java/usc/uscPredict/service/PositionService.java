package usc.uscPredict.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import usc.uscPredict.exception.PositionNotFoundException;
import usc.uscPredict.model.Position;
import usc.uscPredict.repository.PositionRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Service layer for Position management.
 * Handles business logic for user positions in markets.
 */
@Service
public class PositionService {

    private final PositionRepository positionsRepository;

    @Autowired
    public PositionService(PositionRepository positionsRepository) {
        this.positionsRepository = positionsRepository;
    }

    /**
     * Retrieves all positions.
     * @return Set of all positions
     */
    public Set<Position> getAllPositions() {
        Set<Position> positions = new HashSet<>();
        positionsRepository.findAll().forEach(positions::add);
        return positions;
    }

    /**
     * Retrieves a position by UUID.
     * @param uuid The position UUID
     * @return The position if found
     * @throws PositionNotFoundException if the position is not found
     */
    public Position getPositionById(UUID uuid) {
        return positionsRepository.findById(uuid)
                .orElseThrow(() -> new PositionNotFoundException("Position not found with ID: " + uuid));
    }

    /**
     * Retrieves all positions for a specific user.
     * @param userId The user UUID
     * @return Set of positions for the user
     */
    public Set<Position> getPositionsByUserId(UUID userId) {
        return positionsRepository.findByUserId(userId);
    }

    /**
     * Retrieves all positions for a specific market.
     * @param marketId The market UUID
     * @return Set of positions in the market
     */
    public Set<Position> getPositionsByMarketId(UUID marketId) {
        return positionsRepository.findByMarketId(marketId);
    }

    /**
     * Retrieves a position for a specific user and market.
     * @param userId The user UUID
     * @param marketId The market UUID
     * @return The position if found
     * @throws PositionNotFoundException if the position is not found
     */
    public Position getPositionByUserIdAndMarketId(UUID userId, UUID marketId) {
        return positionsRepository.findByUserIdAndMarketId(userId, marketId)
                .orElseThrow(() -> new PositionNotFoundException(
                        "Position not found for user " + userId + " and market " + marketId));
    }

    /**
     * Creates a new position.
     * @param position The position to create
     * @return The created position
     */
    @Transactional
    public Position createPosition(Position position) {
        return positionsRepository.save(position);
    }

    /**
     * Updates an existing position.
     * @param uuid The position UUID
     * @param position The updated position data
     * @return The updated position
     * @throws PositionNotFoundException if the position is not found
     */
    @Transactional
    public Position updatePosition(UUID uuid, Position position) {
        if (!positionsRepository.existsById(uuid)) {
            throw new PositionNotFoundException("Position not found with ID: " + uuid);
        }
        position.setUuid(uuid);
        return positionsRepository.save(position);
    }

    /**
     * Deletes a position.
     * @param uuid The position UUID
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean deletePosition(UUID uuid) {
        if (positionsRepository.existsById(uuid)) {
            positionsRepository.deleteById(uuid);
            return true;
        }
        return false;
    }
}
