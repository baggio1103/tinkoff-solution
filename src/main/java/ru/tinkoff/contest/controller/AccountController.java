package ru.tinkoff.contest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.tinkoff.contest.data.AccountRequest;
import ru.tinkoff.contest.data.AccountResponse;
import ru.tinkoff.contest.data.AccountUpdateRequest;
import ru.tinkoff.contest.service.AccountService;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("")
    public String createAccount(@RequestBody AccountRequest accountRequest) {
        return accountService.createAccount(accountRequest);
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse getAccount(@PathVariable("accountNumber") String accountNumber) {
       return accountService.findAccount(accountNumber);
    }

    @PostMapping("/{accountNumber}/top-up")
    public void depositAccount(@PathVariable("accountNumber") String accountNumber,
                                 @RequestBody AccountUpdateRequest accountRequest) {
        accountService.deposit(accountNumber, accountRequest);
    }

}
