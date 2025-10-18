package usc.uscPredict.repository;

import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usc.uscPredict.model.Order;
import usc.uscPredict.model.OrderSide;
import usc.uscPredict.model.OrderState;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface OrderRepository extends CrudRepository<@NonNull Order, @NonNull UUID> {

    @NonNull
    Set<Order> findAll();

    Set<Order> findByUserId(@NonNull UUID userId);

    Set<Order> findByMarketId(@NonNull UUID marketId);

    Set<Order> findByMarketIdAndState(@NonNull UUID marketId, @NonNull OrderState state);

    /**
     * Retrieves all PENDING BUY orders for a market, sorted by price DESC (highest first).
     * Used for matching: buyers willing to pay more should be matched first.
     * @param marketId The market UUID
     * @param side The order side (BUY)
     * @param state The order state (PENDING)
     * @return List of buy orders sorted by price descending
     */
    @Query("SELECT o FROM Order o WHERE o.marketId = ?1 AND o.side = ?2 AND o.state = ?3 ORDER BY o.price DESC, o.createdAt ASC")
    List<Order> findByMarketIdAndSideAndStateOrderByPriceDesc(
            @NonNull UUID marketId,
            @NonNull OrderSide side,
            @NonNull OrderState state
    );

    /**
     * Retrieves all PENDING SELL orders for a market, sorted by price ASC (lowest first).
     * Used for matching: sellers willing to accept less should be matched first.
     * @param marketId The market UUID
     * @param side The order side (SELL)
     * @param state The order state (PENDING)
     * @return List of sell orders sorted by price ascending
     */
    @Query("SELECT o FROM Order o WHERE o.marketId = ?1 AND o.side = ?2 AND o.state = ?3 ORDER BY o.price ASC, o.createdAt ASC")
    List<Order> findByMarketIdAndSideAndStateOrderByPriceAsc(
            @NonNull UUID marketId,
            @NonNull OrderSide side,
            @NonNull OrderState state
    );
}
