package ru.tinkoff.contest.data;

import ru.tinkoff.contest.model.Currency;

import java.math.BigDecimal;

public record AccountResponse(
        BigDecimal amount,
        Currency currency
) {
}
