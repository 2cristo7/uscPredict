package usc.uscPredict.repository;

import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usc.uscPredict.model.Order;
import usc.uscPredict.model.OrderState;

import java.util.Set;
import java.util.UUID;

@Repository
public interface OrderRepository extends CrudRepository<@NonNull Order, @NonNull UUID> {

    @NonNull
    Set<Order> findAll();

    Set<Order> findByUserId(@NonNull UUID userId);

    Set<Order> findByMarketId(@NonNull UUID marketId);

    Set<Order> findByMarketIdAndState(@NonNull UUID marketId, @NonNull OrderState state);
}
