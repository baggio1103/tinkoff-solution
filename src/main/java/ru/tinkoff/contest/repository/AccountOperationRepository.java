package ru.tinkoff.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tinkoff.contest.model.AccountEntity;
import ru.tinkoff.contest.model.AccountOperationEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AccountOperationRepository extends JpaRepository<AccountOperationEntity, UUID> {

    List<AccountOperationEntity> findByAccount(AccountEntity account);

    List<AccountOperationEntity> findByAccountAndTopUpDateBetween(AccountEntity account,
                                                                  LocalDateTime from,
                                                                  LocalDateTime to);

    List<AccountOperationEntity> findByAccountAndTopUpDateIsAfter(AccountEntity account,
                                                                  LocalDateTime localDateTime);

    List<AccountOperationEntity> findByAccountAndTopUpDateIsBefore(AccountEntity account,
                                                                  LocalDateTime localDateTime);

}
