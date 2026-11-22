package com.ahmedkh.banking.account.service;

import com.ahmedkh.banking.account.dto.AccountResponse;
import com.ahmedkh.banking.account.dto.CreateAccountRequest;
import com.ahmedkh.banking.account.dto.InternalAccountResponse;
import com.ahmedkh.banking.account.entity.AccountStatus;
import com.ahmedkh.banking.account.entity.AccountType;
import com.ahmedkh.banking.account.entity.BankAccount;
import com.ahmedkh.banking.account.exception.CurrencyMismatchException;
import com.ahmedkh.banking.account.exception.InsufficientBalanceException;
import com.ahmedkh.banking.account.exception.InvalidAccountStateException;
import com.ahmedkh.banking.account.repository.BankAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private BankAccountRepository repository;

    @InjectMocks
    private AccountService service;

    @Test
    void createNormalizesCurrencyAndBalance() {
        UUID owner = UUID.randomUUID();
        when(repository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = service.create(new CreateAccountRequest(
                owner,
                AccountType.CHECKING,
                "eur",
                new BigDecimal("10.5")
        ));

        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
        verify(repository).save(captor.capture());
        BankAccount saved = captor.getValue();
        assertThat(saved.getOwnerUserId()).isEqualTo(owner);
        assertThat(saved.getCurrency()).isEqualTo("EUR");
        assertThat(saved.getBalance()).isEqualByComparingTo("10.50");
        assertThat(saved.getReservedBalance()).isEqualByComparingTo("0.00");
        assertThat(saved.getAccountNumberMasked()).matches("FR76 \\*\\*\\*\\* \\*\\*\\*\\* \\*\\*\\*\\* \\d{4}");
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void reserveLocksAccountAndTracksReservedBalance() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(
                account(id, AccountStatus.ACTIVE, "100.00", "0.00", "EUR")
        ));
        when(repository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InternalAccountResponse response = service.reserve(id, new BigDecimal("25.50"), "EUR");

        verify(repository).findByIdForUpdate(id);
        assertThat(response.reservedBalance()).isEqualByComparingTo("25.50");
        assertThat(response.availableBalance()).isEqualByComparingTo("74.50");
        assertThat(response.balance()).isEqualByComparingTo("100.00");
    }

    @Test
    void debitRequiresAnExistingReservation() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(
                account(id, AccountStatus.ACTIVE, "100.00", "0.00", "EUR")
        ));

        assertThatThrownBy(() -> service.debit(id, new BigDecimal("10.00"), "EUR"))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void suspendedAccountIsRejected() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(
                account(id, AccountStatus.SUSPENDED, "100.00", "0.00", "EUR")
        ));

        assertThatThrownBy(() -> service.reserve(id, new BigDecimal("10.00"), "EUR"))
                .isInstanceOf(InvalidAccountStateException.class);
    }

    @Test
    void currencyMismatchIsRejected() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(
                account(id, AccountStatus.ACTIVE, "100.00", "0.00", "EUR")
        ));

        assertThatThrownBy(() -> service.credit(id, new BigDecimal("10.00"), "USD"))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void amountsWithMoreThanTwoDecimalsAreRejected() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.reserve(id, new BigDecimal("1.234"), "EUR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("two decimal places");
    }

    private BankAccount account(
            UUID id,
            AccountStatus status,
            String balance,
            String reservedBalance,
            String currency
    ) {
        return new BankAccount(
                id,
                UUID.randomUUID(),
                "FR76 **** **** **** 1234",
                AccountType.CHECKING,
                currency,
                status,
                new BigDecimal(balance),
                new BigDecimal(reservedBalance),
                Instant.now()
        );
    }
}
