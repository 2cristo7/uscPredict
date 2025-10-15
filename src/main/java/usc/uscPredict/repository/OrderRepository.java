package usc.uscPredict.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import usc.uscPredict.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    /**
     * Buscar todas las órdenes de un evento específico
     */
    List<Order> findByEvent_Id(UUID eventId);

    /**
     * Buscar una orden por ID que pertenezca a un evento específico
     */
    Optional<Order> findByIdAndEvent_Id(UUID orderId, UUID eventId);
}
