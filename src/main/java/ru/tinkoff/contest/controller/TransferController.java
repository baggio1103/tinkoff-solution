package ru.tinkoff.contest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tinkoff.contest.data.TransferRequest;
import ru.tinkoff.contest.service.AccountService;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final AccountService accountService;

    @PostMapping
    public void transfer(@RequestBody TransferRequest transferRequest) {
        accountService.transfer(transferRequest);
    }

}
