package ru.tinkoff.contest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.tinkoff.contest.data.AccountResponse;
import ru.tinkoff.contest.service.AccountService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account-turnover")
public class Analytics {

    private final AccountService accountService;

    @GetMapping("/{accountNumber}")
    public AccountResponse getAccountTurnOver(@PathVariable("accountNumber") String accountNumber,
                                              @RequestParam(required = false) String startDate,
                                              @RequestParam(required = false) String endDate) {
        return accountService.accountTurnOver(accountNumber, startDate, endDate);
    }

}
