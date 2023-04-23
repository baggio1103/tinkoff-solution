package ru.tinkoff.contest.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.tinkoff.contest.data.AccountRequest;
import ru.tinkoff.contest.data.AccountResponse;
import ru.tinkoff.contest.data.AccountUpdateRequest;
import ru.tinkoff.contest.data.TransferRequest;
import ru.tinkoff.contest.exception.AccountNotFoundException;
import ru.tinkoff.contest.model.AccountEntity;
import ru.tinkoff.contest.model.AccountOperationEntity;
import ru.tinkoff.contest.model.Currency;
import ru.tinkoff.contest.repository.AccountOperationRepository;
import ru.tinkoff.contest.repository.AccountRepository;
import ru.tinkoff.contest.repository.CurrencyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static ru.tinkoff.contest.constants.DateTimeFormats.DATE_FORMAT;
import static ru.tinkoff.contest.constants.DateTimeFormats.DATE_TIME_FORMAT;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    private final AccountOperationRepository accountOperationRepository;

    private final CurrencyRepository currencyRepository;

    public String createAccount(AccountRequest accountRequest) {
        validate(accountRequest.firstName(),
                accountRequest.lastName(),
                accountRequest.country(),
                accountRequest.birthDay());
        var accountNumber = generateAccountNumber();
        var accountEntity = new AccountEntity(
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                accountNumber,
                accountRequest.firstName(),
                accountRequest.lastName(),
                accountRequest.country(),
                BigDecimal.ZERO,
                Currency.valueOf(accountRequest.currency()),
                LocalDate.parse(accountRequest.birthDay(), DateTimeFormatter.ofPattern(DATE_FORMAT))
        );
        accountRepository.save(accountEntity);
        return accountNumber;
    }

    private void validate(String firstName, String lastName, String country, String birthday) {
        if (StringUtils.isBlank(firstName) || StringUtils.isBlank(lastName) || StringUtils.isEmpty(country)) {
            throw new IllegalArgumentException("Required fields mustn't be null");
        }
        try {
            var value = LocalDate.parse(birthday, DateTimeFormatter.ofPattern(DATE_FORMAT));
            var now = LocalDate.now();
            var yearDiff = Math.abs(ChronoUnit.YEARS.between(now, value));
            if (yearDiff < 14 || yearDiff > 114) {
                log.info("Age: {}", yearDiff);
                throw new IllegalArgumentException("Birthday value is wrong. Age " + yearDiff + " is not suitable");
            }
        } catch (DateTimeParseException dateTimeParseException) {
            throw new IllegalArgumentException("Date Format is wrong");
        }
    }

    public AccountEntity findAccountEntity(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + accountNumber));
    }
    public AccountResponse findAccount(String accountNumber) {
        var account = findAccountEntity(accountNumber);
        return new AccountResponse(account.getBalance(), account.getCurrency());
    }

    private String generateAccountNumber() {
        while (true) {
            var accountNumber = RandomStringUtils.randomNumeric(10);
            if (accountRepository.findByAccountNumber(accountNumber).isEmpty()) {
                return accountNumber;
            }
        }
    }

    public void deposit(String accountNumber, AccountUpdateRequest accountRequest) {
        var account = findAccountEntity(accountNumber);
        if (accountRequest.amount().doubleValue() < 0) {
            throw new IllegalArgumentException("Amount must greater than zero");
        }
        AccountOperationEntity accountOperationEntity = new AccountOperationEntity(
                UUID.randomUUID(),
                LocalDateTime.parse(accountRequest.topUpDate(), DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)),
                accountRequest.amount(),
                account.getBalance(),
                account
        );
        accountOperationRepository.save(accountOperationEntity);
        account.setBalance(account.getBalance().add(accountRequest.amount()));
        accountRepository.save(account);
    }

    @Transactional
    public void transfer(TransferRequest transferRequest) {
        var receiver = findAccountEntity(transferRequest.receiverAccount());
        var sender = findAccountEntity(transferRequest.senderAccount());
        if (transferRequest.amountInSenderCurrency().doubleValue() < 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (sender.getBalance().compareTo(transferRequest.amountInSenderCurrency()) < 0) {
            throw new IllegalArgumentException("SenderAccount has no sufficient balance");
        }
        var transferDate = LocalDateTime.parse(transferRequest.transferDate(),
                DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));

        //TODO value in currency - transferRequest.amountInSenderCurrency()
        AccountOperationEntity senderOperation = new AccountOperationEntity(
                UUID.randomUUID(),
                transferDate,
                transferRequest.amountInSenderCurrency().negate(),
                sender.getBalance(),
                sender
        );
        sender.setBalance(sender.getBalance().min(transferRequest.amountInSenderCurrency()));
        accountOperationRepository.save(senderOperation);
        accountRepository.save(sender);

        if (receiver.getCurrency() == sender.getCurrency()) {
            AccountOperationEntity receiverOperation = new AccountOperationEntity(
                    UUID.randomUUID(),
                    transferDate,
                    transferRequest.amountInSenderCurrency(),
                    receiver.getBalance(),
                    receiver
            );
            accountOperationRepository.save(receiverOperation);
            receiver.setBalance(receiver.getBalance().add(transferRequest.amountInSenderCurrency()));
            accountRepository.save(receiver);
        } else {
            // rub usd
            // usd rub
            // USDRUB 4.40
            var currencyId = sender.getCurrency().toString() + receiver.getCurrency().toString();
            var currencyRate = currencyRepository.findById(currencyId);
            BigDecimal convertedAmount;
            if (currencyRate.isEmpty()) {
                currencyId = receiver.getCurrency().toString() + sender.getCurrency().toString();
                var temp  = currencyRepository.findById(currencyId).get();
                convertedAmount = transferRequest.amountInSenderCurrency().multiply(
                        BigDecimal.valueOf(BigDecimal.ONE.divide(BigDecimal.valueOf(temp.getValue())).setScale(2, RoundingMode.HALF_EVEN))
            } else {
                convertedAmount = transferRequest.amountInSenderCurrency().multiply(
                        BigDecimal.valueOf(currencyRate.get().getValue())).setScale(2, RoundingMode.HALF_EVEN);
            }
//            var currencyRate  = currencyRepository.findById(currencyId).orElseThrow(
//                    () -> new IllegalArgumentException("CurrencyId is absent " + currencyId)
//            );
            AccountOperationEntity receiverOperation = new AccountOperationEntity(
                    UUID.randomUUID(),
                    transferDate,
                    convertedAmount,
                    receiver.getBalance(),
                    receiver
            );
            accountOperationRepository.save(receiverOperation);
            receiver.setBalance(receiver.getBalance().add(convertedAmount));
            accountRepository.save(receiver);
        }
    }

    public AccountResponse accountTurnOver(String accountNumber, String startDate, String endDate) {
        if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
            var start = LocalDateTime.parse(startDate,
                    DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
            var end = LocalDateTime.parse(endDate,
                    DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("Start date cannot be after End date");
            }
            var account = findAccountEntity(accountNumber);
            var overallAmount = accountOperationRepository.findByAccountAndTopUpDateBetween(account, start, end)
                    .stream().map(AccountOperationEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new AccountResponse(overallAmount, account.getCurrency());
        }
        if (StringUtils.isNotBlank(startDate)) {
            var start = LocalDateTime.parse(startDate,
                    DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
            var account = findAccountEntity(accountNumber);
            var overallAmount = accountOperationRepository.findByAccountAndTopUpDateIsAfter(account, start)
                    .stream().map(AccountOperationEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new AccountResponse(overallAmount, account.getCurrency());
        }
        if (StringUtils.isNotBlank(endDate)) {
            var end = LocalDateTime.parse(endDate,
                    DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
            var account = findAccountEntity(accountNumber);
            var overallAmount = accountOperationRepository.findByAccountAndTopUpDateIsBefore(account, end)
                    .stream().map(AccountOperationEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new AccountResponse(overallAmount, account.getCurrency());
        }
        var account = findAccountEntity(accountNumber);
        var overallAmount = accountOperationRepository.findByAccount(account)
                .stream().map(AccountOperationEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AccountResponse(overallAmount, account.getCurrency());
    }

}
