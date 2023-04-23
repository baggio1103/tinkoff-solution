package ru.tinkoff.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tinkoff.contest.model.AccountEntity;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByAccountNumber(String accountNumber);

}
