package usc.uscPredict.repository;

import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usc.uscPredict.model.User;
import java.util.Set;

@Repository
public interface UserRepository extends CrudRepository<@NonNull User,@NonNull String> {

    @NonNull
    Set<User> findAll();

    @Query
    Set<User> findByName(@NonNull String name);
}
