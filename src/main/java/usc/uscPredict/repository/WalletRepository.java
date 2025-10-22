package usc.uscPredict.repository;

import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usc.uscPredict.model.Wallet;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface WalletRepository extends CrudRepository<@NonNull Wallet, @NonNull UUID> {

    @NonNull
    Set<Wallet> findAll();

    Optional<Wallet> findByUserId(@NonNull UUID userId);
}
