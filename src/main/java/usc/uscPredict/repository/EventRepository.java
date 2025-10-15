package usc.uscPredict.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import usc.uscPredict.model.Event;
import usc.uscPredict.model.EventState;
import usc.uscPredict.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    // Buscar eventos por estado
    List<Event> findByState(EventState state);

    // Buscar eventos por creador
    List<Event> findByCreatedBy(User user);

    // Buscar eventos por creador (usando UUID)
    List<Event> findByCreatedBy_Uuid(UUID creatorUuid);

    // Buscar eventos entre fechas
    @Query("SELECT e FROM Event e WHERE e.startDate >= :startDate AND e.startDate <= :endDate")
    List<Event> findByStartDateBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // Buscar eventos por estado y creador
    List<Event> findByStateAndCreatedBy(EventState state, User user);

    // Buscar eventos por estado y creador (usando UUID)
    List<Event> findByStateAndCreatedBy_Uuid(EventState state, UUID creatorUuid);

    // Buscar eventos activos que han comenzado pero no terminado
    @Query("SELECT e FROM Event e WHERE e.state = :state AND e.startDate <= :now AND e.endDate >= :now")
    List<Event> findActiveEventsInProgress(@Param("state") EventState state,
                                          @Param("now") LocalDateTime now);

    // Buscar eventos que deben cerrarse (fecha de fin pasada pero aún activos)
    @Query("SELECT e FROM Event e WHERE e.state = 'ACTIVE' AND e.endDate < :now")
    List<Event> findEventsToClose(@Param("now") LocalDateTime now);
}
