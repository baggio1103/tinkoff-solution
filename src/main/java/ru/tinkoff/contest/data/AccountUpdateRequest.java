package ru.tinkoff.contest.data;

import java.math.BigDecimal;

public record AccountUpdateRequest(
    BigDecimal amount,
    String topUpDate
) {
}
