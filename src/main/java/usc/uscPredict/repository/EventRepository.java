package usc.uscPredict.repository;

import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usc.uscPredict.model.Event;
import usc.uscPredict.model.EventState;

import java.util.Set;
import java.util.UUID;

@Repository
public interface EventRepository extends CrudRepository<@NonNull Event, @NonNull UUID> {

    @NonNull
    Set<Event> findAll();

    Set<Event> findByState(@NonNull EventState state);
}
