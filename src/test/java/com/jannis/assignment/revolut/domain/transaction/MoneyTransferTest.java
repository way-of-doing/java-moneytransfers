package com.jannis.assignment.revolut.domain.transaction;

import com.jannis.assignment.revolut.domain.account.Account;
import com.jannis.assignment.revolut.domain.account.AccountId;
import com.jannis.assignment.revolut.domain.account.AccountResolver;
import com.jannis.assignment.revolut.domain.transaction.execution.TransactionExecutionContext;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTransferTest {
    @Test
    void cannotCreateMoneyTransferWithNullArguments() {
        final var accountId = new AccountId("foo");
        final var money = Money.of(0, "EUR");

        assertThrows(NullPointerException.class, () -> new MoneyTransfer(null, accountId, money));
        assertThrows(NullPointerException.class, () -> new MoneyTransfer(accountId, null, money));
        assertThrows(NullPointerException.class, () -> new MoneyTransfer(accountId, accountId, null));
    }

    @Test
    void getInvolvedAccountsReturnsSourceAndDestination() {
        final var sourceId = new AccountId("source");
        final var destinationId = new AccountId("destination");
        final var transfer = new MoneyTransfer(sourceId, destinationId, Money.of(0, "EUR"));

        assertEquals(
                Set.of(sourceId, destinationId),
                transfer.getInvolvedAccounts().collect(Collectors.toUnmodifiableSet())
        );
    }

    @Test
    void transfersBetweenDifferentCurrencyAccountsAreNotAllowed() {
        final var source = Account.of("source", "EUR", 100);
        final var destination = Account.of("destination ", "USD", 100);
        final var transfer = new MoneyTransfer(source.getId(), destination.getId(), Money.of(0, "EUR"));

        assertThrows(
                TransactionException.class,
                () -> transfer.execute(getExecutionContextOf(source, destination)),
                "Transfers between accounts denominated in different currencies are not allowed"
        );
    }

    @Test
    void transferCurrencyMustMatchSourceAccountCurrency() {
        final var source = Account.of("source", "EUR", 100);
        final var destination = Account.of("destination ", "EUR", 100);
        final var transfer = new MoneyTransfer(source.getId(), destination.getId(), Money.of(0, "USD"));

        assertThrows(
                TransactionException.class,
                () -> transfer.execute(getExecutionContextOf(source, destination)),
                "Transfers are only allowed for the source account's denominated currency"
        );
    }

    @Test
    void transferMovesMoneyAround() {
        final var source = Account.of("source", "EUR", 100);
        final var destination = Account.of("destination ", "EUR", 100);
        final var transfer = new MoneyTransfer(source.getId(), destination.getId(), Money.of(50, "EUR"));

        transfer.execute(getExecutionContextOf(source, destination));

        assertEquals(Money.of(50, "EUR"), source.getBalance());
        assertEquals(Money.of(150, "EUR"), destination.getBalance());
    }

    @Test
    void transferFailsWithNoSideEffectsIfSourceHasNotEnoughMoney() {
        final var source = Account.of("source", "EUR", 100);
        final var destination = Account.of("destination ", "EUR", 100);
        final var transfer = new MoneyTransfer(source.getId(), destination.getId(), Money.of(200, "EUR"));

        assertThrows(
                TransactionException.class,
                () -> transfer.execute(getExecutionContextOf(source, destination))
        );

        assertEquals(Money.of(100, "EUR"), source.getBalance());
        assertEquals(Money.of(100, "EUR"), destination.getBalance());
    }

    private static TransactionExecutionContext getExecutionContextOf(Account... accounts) {
        final var map = new HashMap<AccountId, Account>();
        for (var account : accounts) {
            map.put(account.getId(), account);
        }

        return new TransactionExecutionContext() {
            @Override
            public AccountResolver getAccountResolver() {
                return map::get;
            }

            @Override
            public boolean acquireTransactionLock(AccountId accountId) {
                return true;
            }

            @Override
            public boolean releaseTransactionLock(AccountId accountId) {
                return true;
            }
        };
    }
}
