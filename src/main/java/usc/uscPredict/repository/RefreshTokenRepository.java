package usc.uscPredict.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usc.uscPredict.model.RefreshToken;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    Collection<RefreshToken> deleteAllByUserEmail(String userEmail);
    Optional<RefreshToken> findByToken(String token);
}
