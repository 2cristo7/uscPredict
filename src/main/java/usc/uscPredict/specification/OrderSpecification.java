package usc.uscPredict.specification;

import org.springframework.data.jpa.domain.Specification;
import usc.uscPredict.model.Order;
import usc.uscPredict.model.OrderState;
import usc.uscPredict.model.OrderType;

import java.util.UUID;

/**
 * Especificaciones para filtrado dinámico de órdenes usando Spring Data JPA Specifications
 */
public class OrderSpecification {

    /**
     * Filtrar órdenes por evento
     */
    public static Specification<Order> belongsToEvent(UUID eventId) {
        return (root, query, criteriaBuilder) -> {
            if (eventId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("event").get("id"), eventId);
        };
    }

    /**
     * Filtrar órdenes por estado
     */
    public static Specification<Order> hasState(OrderState state) {
        return (root, query, criteriaBuilder) -> {
            if (state == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("state"), state);
        };
    }

    /**
     * Filtrar órdenes por tipo (BUY/SELL)
     */
    public static Specification<Order> hasType(OrderType type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("type"), type);
        };
    }

    /**
     * Filtrar órdenes por usuario
     */
    public static Specification<Order> hasUserId(UUID userId) {
        return (root, query, criteriaBuilder) -> {
            if (userId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("user").get("uuid"), userId);
        };
    }

    /**
     * Combinar todas las especificaciones
     */
    public static Specification<Order> withFilters(UUID eventId, OrderState state, OrderType type, UUID userId) {
        return Specification.where(belongsToEvent(eventId))
                .and(hasState(state))
                .and(hasType(type))
                .and(hasUserId(userId));
    }
}
