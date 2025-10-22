package usc.uscPredict.repository;

import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import usc.uscPredict.model.Transaction;

import java.util.Set;
import java.util.UUID;

@Repository
public interface TransactionRepository extends CrudRepository<@NonNull Transaction, @NonNull UUID> {

    @NonNull
    Set<Transaction> findAll();

    Set<Transaction> findByUserId(@NonNull UUID userId);

    Set<Transaction> findByUserIdOrderByCreatedAtDesc(@NonNull UUID userId);
}
