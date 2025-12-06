package usc.uscPredict.repository;

import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usc.uscPredict.model.User;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserRepository extends CrudRepository<@NonNull User,@NonNull UUID> {

    @NonNull
    Set<User> findAll();

    @Query
    Set<User> findByName(@NonNull String name);

    Optional<User> findByEmail(String email);
}
