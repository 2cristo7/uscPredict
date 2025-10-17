package usc.uscPredict.repository;

import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usc.uscPredict.model.Market;
import usc.uscPredict.model.MarketStatus;

import java.util.Set;
import java.util.UUID;

@Repository
public interface MarketRepository extends CrudRepository<@NonNull Market, @NonNull UUID> {

    @NonNull
    Set<Market> findAll();

    Set<Market> findByEventId(@NonNull UUID eventId);

    Set<Market> findByStatus(@NonNull MarketStatus status);
}
