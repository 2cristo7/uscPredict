package usc.uscPredict.service;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import usc.uscPredict.dto.CreateOrderRequest;
import usc.uscPredict.exception.EventNotFoundException;
import usc.uscPredict.exception.OrderNotFoundException;
import usc.uscPredict.model.*;
import usc.uscPredict.repository.EventRepository;
import usc.uscPredict.repository.OrderRepository;
import usc.uscPredict.repository.UserRepository;
import usc.uscPredict.specification.OrderSpecification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventService eventService;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                       EventRepository eventRepository,
                       UserRepository userRepository,
                       EventService eventService) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
    }

    /**
     * Obtener todas las órdenes de un evento con filtros opcionales
     */
    public List<Order> getAllOrdersForEvent(@NonNull UUID eventId,
                                            OrderState state,
                                            OrderType type,
                                            UUID userId) {
        // Verificar que el evento existe
        eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + eventId));

        // Crear especificación combinada con todos los filtros
        Specification<Order> spec = OrderSpecification.withFilters(eventId, state, type, userId);

        // Ejecutar consulta con especificaciones
        return orderRepository.findAll(spec);
    }

    /**
     * Obtener una orden específica de un evento
     */
    public Order getOrderById(@NonNull UUID eventId, @NonNull UUID orderId) {
        return orderRepository.findByIdAndEvent_Id(orderId, eventId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + orderId + " for event: " + eventId));
    }

    /**
     * Crear una nueva orden en un evento
     */
    public Order createOrder(@NonNull UUID eventId, @NonNull CreateOrderRequest request) {
        // Verificar que el evento existe
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + eventId));

        // Verificar que el usuario existe
        User user = userRepository.findById(request.getUserId().toString())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.getUserId()));

        // Validar precio (debe estar entre 0 y 1 para mercados de predicción)
        if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0 ||
            request.getPrice().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Price must be between 0 and 1");
        }

        // Crear la orden
        Order order = new Order(event, user, request.getType(), request.getPrice(), request.getQuantity());
        order.setState(OrderState.PENDING);

        // Guardar la orden
        Order savedOrder = orderRepository.save(order);

        // Agregar la orden al order book del evento correspondiente
        String orderBookType = determineOrderBookType(request.getType());
        eventService.addOrderToEvent(eventId, savedOrder.getId(), orderBookType);

        return savedOrder;
    }

    /**
     * Determinar a qué order book agregar la orden basado en el tipo
     */
    private String determineOrderBookType(OrderType type) {
        // Por simplicidad, asumimos que BUY va a YES y SELL va a NO
        // Esto puede ajustarse según la lógica de negocio
        return type == OrderType.BUY ? "yes" : "no";
    }

    /**
     * Actualización parcial de una orden
     */
    public Order partialUpdateOrder(@NonNull UUID eventId,
                                   @NonNull UUID orderId,
                                   @NonNull Map<String, Object> updates) {
        Order order = getOrderById(eventId, orderId);

        updates.forEach((key, value) -> {
            switch (key) {
                case "state":
                    OrderState newState = OrderState.valueOf((String) value);
                    order.setState(newState);
                    break;
                case "price":
                    BigDecimal newPrice = new BigDecimal(value.toString());
                    if (newPrice.compareTo(BigDecimal.ZERO) <= 0 ||
                        newPrice.compareTo(BigDecimal.ONE) > 0) {
                        throw new IllegalArgumentException("Price must be between 0 and 1");
                    }
                    order.setPrice(newPrice);
                    break;
                case "quantity":
                    int newQuantity = ((Number) value).intValue();
                    if (newQuantity <= 0) {
                        throw new IllegalArgumentException("Quantity must be positive");
                    }
                    order.setQuantity(newQuantity);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown field: " + key);
            }
        });

        return orderRepository.save(order);
    }

    /**
     * Eliminar o cancelar una orden (soft delete)
     */
    public void deleteOrder(@NonNull UUID eventId, @NonNull UUID orderId) {
        Order order = getOrderById(eventId, orderId);

        // Soft delete: cambiar estado a CANCELLED
        order.setState(OrderState.CANCELLED);
        orderRepository.save(order);
    }

    /**
     * Eliminación permanente (solo si es necesario)
     */
    public void hardDeleteOrder(@NonNull UUID eventId, @NonNull UUID orderId) {
        Order order = getOrderById(eventId, orderId);
        orderRepository.delete(order);
    }

    /**
     * Ejecutar una orden (cambiar estado a EXECUTED)
     */
    public Order executeOrder(@NonNull UUID eventId, @NonNull UUID orderId) {
        Order order = getOrderById(eventId, orderId);

        if (order.getState() == OrderState.EXECUTED) {
            throw new IllegalStateException("Order is already executed");
        }

        if (order.getState() == OrderState.CANCELLED) {
            throw new IllegalStateException("Cannot execute a cancelled order");
        }

        order.setState(OrderState.EXECUTED);
        return orderRepository.save(order);
    }

    /**
     * Obtener estadísticas de órdenes para un evento
     */
    public Map<String, Object> getOrderStats(@NonNull UUID eventId) {
        List<Order> allOrders = orderRepository.findByEvent_Id(eventId);

        long pendingCount = allOrders.stream()
                .filter(o -> o.getState() == OrderState.PENDING)
                .count();
        long executedCount = allOrders.stream()
                .filter(o -> o.getState() == OrderState.EXECUTED)
                .count();
        long cancelledCount = allOrders.stream()
                .filter(o -> o.getState() == OrderState.CANCELLED)
                .count();
        long buyCount = allOrders.stream()
                .filter(o -> o.getType() == OrderType.BUY)
                .count();
        long sellCount = allOrders.stream()
                .filter(o -> o.getType() == OrderType.SELL)
                .count();

        return Map.of(
                "eventId", eventId,
                "totalOrders", allOrders.size(),
                "pendingOrders", pendingCount,
                "executedOrders", executedCount,
                "cancelledOrders", cancelledCount,
                "buyOrders", buyCount,
                "sellOrders", sellCount
        );
    }
}
