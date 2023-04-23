package ru.tinkoff.contest.repository;

import org.springframework.data.repository.CrudRepository;
import ru.tinkoff.contest.model.CurrencyEntity;

public interface CurrencyRepository extends CrudRepository<CurrencyEntity, String> {
}
