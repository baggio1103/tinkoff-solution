package ru.tinkoff.contest.data;

import java.math.BigDecimal;

public record TransferRequest(
        String receiverAccount,
        String senderAccount,
        BigDecimal amountInSenderCurrency,

        String transferDate
) {
}
