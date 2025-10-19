package usc.uscPredict.repository;

import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usc.uscPredict.model.Position;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PositionRepository extends CrudRepository<@NonNull Position, @NonNull UUID> {

    @NonNull
    Set<Position> findAll();

    Set<Position> findByUserId(@NonNull UUID userId);

    Set<Position> findByMarketId(@NonNull UUID marketId);

    Optional<Position> findByUserIdAndMarketId(@NonNull UUID userId, @NonNull UUID marketId);
}
