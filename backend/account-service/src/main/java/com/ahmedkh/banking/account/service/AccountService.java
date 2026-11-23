package com.ahmedkh.banking.account.service;

import com.ahmedkh.banking.account.dto.AccountResponse;
import com.ahmedkh.banking.account.dto.CreateAccountRequest;
import com.ahmedkh.banking.account.dto.InternalAccountResponse;
import com.ahmedkh.banking.account.dto.InternalExternalCreditRequest;
import com.ahmedkh.banking.account.dto.InternalExternalCreditResponse;
import com.ahmedkh.banking.account.dto.UpdateAccountStatusRequest;
import com.ahmedkh.banking.account.entity.AccountStatus;
import com.ahmedkh.banking.account.entity.BankAccount;
import com.ahmedkh.banking.account.entity.ExternalAccount;
import com.ahmedkh.banking.account.entity.ExternalAccountCredit;
import com.ahmedkh.banking.account.exception.AccountNotFoundException;
import com.ahmedkh.banking.account.exception.CurrencyMismatchException;
import com.ahmedkh.banking.account.exception.InsufficientBalanceException;
import com.ahmedkh.banking.account.exception.InvalidAccountStateException;
import com.ahmedkh.banking.account.repository.BankAccountRepository;
import com.ahmedkh.banking.account.repository.ExternalAccountCreditRepository;
import com.ahmedkh.banking.account.repository.ExternalAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AccountService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final BankAccountRepository repository;
    private final ExternalAccountRepository externalAccountRepository;
    private final ExternalAccountCreditRepository externalCreditRepository;

    public AccountService(
            BankAccountRepository repository,
            ExternalAccountRepository externalAccountRepository,
            ExternalAccountCreditRepository externalCreditRepository
    ) {
        this.repository = repository;
        this.externalAccountRepository = externalAccountRepository;
        this.externalCreditRepository = externalCreditRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listForOwner(UUID ownerUserId) {
        return repository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId).stream()
                .map(this::toAccountResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getForOwner(UUID accountId, UUID ownerUserId) {
        BankAccount account = repository.findById(accountId).orElseThrow(AccountNotFoundException::new);
        if (!account.getOwnerUserId().equals(ownerUserId)) {
            throw new AccountNotFoundException();
        }
        return toAccountResponse(account);
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        BigDecimal initialBalance = nonNegativeMoney(request.initialBalance());
        String currency = request.currency().toUpperCase(Locale.ROOT);
        BankAccount account = new BankAccount(
                UUID.randomUUID(),
                request.ownerUserId(),
                generateMaskedNumber(),
                request.accountType(),
                currency,
                AccountStatus.ACTIVE,
                initialBalance,
                ZERO,
                Instant.now()
        );
        return toAccountResponse(repository.save(account));
    }

    @Transactional
    public AccountResponse updateStatus(UUID accountId, UpdateAccountStatusRequest request) {
        BankAccount account = findLocked(accountId);
        account.setStatus(request.status());
        return toAccountResponse(repository.save(account));
    }

    @Transactional
    public InternalAccountResponse reserve(UUID accountId, BigDecimal amount, String currency) {
        BigDecimal value = positiveMoney(amount);
        BankAccount account = findUsableLocked(accountId, currency);
        if (account.getAvailableBalance().compareTo(value) < 0) {
            throw new InsufficientBalanceException();
        }
        account.setReservedBalance(account.getReservedBalance().add(value));
        return toInternalResponse(repository.save(account));
    }

    @Transactional
    public InternalAccountResponse debit(UUID accountId, BigDecimal amount, String currency) {
        BigDecimal value = positiveMoney(amount);
        BankAccount account = findUsableLocked(accountId, currency);
        if (account.getReservedBalance().compareTo(value) < 0) {
            throw new InsufficientBalanceException();
        }
        account.setBalance(account.getBalance().subtract(value));
        account.setReservedBalance(account.getReservedBalance().subtract(value));
        return toInternalResponse(repository.save(account));
    }

    @Transactional
    public InternalAccountResponse credit(UUID accountId, BigDecimal amount, String currency) {
        BigDecimal value = positiveMoney(amount);
        BankAccount account = findUsableLocked(accountId, currency);
        account.setBalance(account.getBalance().add(value));
        return toInternalResponse(repository.save(account));
    }

    @Transactional
    public InternalExternalCreditResponse creditExternal(InternalExternalCreditRequest request) {
        BigDecimal value = positiveMoney(request.amount());
        String reference = maskedReference(request.destinationReference());
        String currency = request.currency().toUpperCase(Locale.ROOT);

        if (externalCreditRepository.existsByOperationId(request.operationId())) {
            ExternalAccount processed = externalAccountRepository.findByDestinationReference(reference)
                    .orElseThrow(AccountNotFoundException::new);
            return new InternalExternalCreditResponse(
                    processed.getId(),
                    reference,
                    processed.getBalance(),
                    true
            );
        }

        Instant now = Instant.now();
        ExternalAccount account = externalAccountRepository.findByDestinationReference(reference)
                .orElseGet(() -> new ExternalAccount(
                        UUID.randomUUID(),
                        reference,
                        currency,
                        ZERO,
                        now,
                        now
                ));
        if (!account.getCurrency().equals(currency)) {
            throw new CurrencyMismatchException();
        }
        account.setBalance(account.getBalance().add(value));
        account.setUpdatedAt(now);
        externalAccountRepository.save(account);
        externalCreditRepository.save(new ExternalAccountCredit(
                request.operationId(),
                account.getId(),
                value,
                currency,
                now
        ));
        return new InternalExternalCreditResponse(account.getId(), reference, account.getBalance(), false);
    }

    @Transactional
    public InternalAccountResponse release(UUID accountId, BigDecimal amount, String currency) {
        BigDecimal value = positiveMoney(amount);
        BankAccount account = findUsableLocked(accountId, currency);
        if (account.getReservedBalance().compareTo(value) < 0) {
            throw new InsufficientBalanceException();
        }
        account.setReservedBalance(account.getReservedBalance().subtract(value));
        return toInternalResponse(repository.save(account));
    }

    private BankAccount findLocked(UUID accountId) {
        return repository.findByIdForUpdate(accountId).orElseThrow(AccountNotFoundException::new);
    }

    private BankAccount findUsableLocked(UUID accountId, String currency) {
        BankAccount account = findLocked(accountId);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountStateException();
        }
        if (!account.getCurrency().equals(currency.toUpperCase(Locale.ROOT))) {
            throw new CurrencyMismatchException();
        }
        return account;
    }

    private BigDecimal nonNegativeMoney(BigDecimal amount) {
        BigDecimal value = scaledMoney(amount);
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Amount must not be negative");
        }
        return value;
    }

    private BigDecimal positiveMoney(BigDecimal amount) {
        BigDecimal value = scaledMoney(amount);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return value;
    }

    private BigDecimal scaledMoney(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Amount must have at most two decimal places");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private String maskedReference(String reference) {
        if (reference == null || !reference.matches("FR76 \\*{4} \\*{4} \\*{4} \\d{4}")) {
            throw new IllegalArgumentException("Destination reference must be masked");
        }
        return reference;
    }

    private String generateMaskedNumber() {
        String raw = UUID.randomUUID().toString().replace("-", "").replaceAll("\\D", "");
        String digits = raw.length() >= 4 ? raw.substring(raw.length() - 4) : "0000";
        return "FR76 **** **** **** " + digits;
    }

    private AccountResponse toAccountResponse(BankAccount account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerUserId(),
                account.getAccountNumberMasked(),
                account.getAccountType(),
                account.getCurrency(),
                account.getStatus(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }

    private InternalAccountResponse toInternalResponse(BankAccount account) {
        return new InternalAccountResponse(
                account.getId(),
                account.getStatus(),
                account.getBalance(),
                account.getReservedBalance(),
                account.getAvailableBalance()
        );
    }
}
