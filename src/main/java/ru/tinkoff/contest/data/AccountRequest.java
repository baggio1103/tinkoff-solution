package ru.tinkoff.contest.data;

public record AccountRequest(
        String firstName,
        String lastName,
        String country,
        String birthDay,
        String currency
) {
}
