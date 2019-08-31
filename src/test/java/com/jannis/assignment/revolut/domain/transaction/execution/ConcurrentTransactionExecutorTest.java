package com.jannis.assignment.revolut.domain.transaction.execution;

import com.jannis.assignment.revolut.domain.account.Account;
import com.jannis.assignment.revolut.domain.account.AccountId;
import com.jannis.assignment.revolut.domain.transaction.MoneyTransfer;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcurrentTransactionExecutorTest {
    @Test
    void simpleTransferExecutes() {
        final var accounts = accountMapOf(
                Account.of("from", "EUR", 100),
                Account.of("to", "EUR", 100)
        );

        final var executor = new ConcurrentTransactionExecutor(accounts::get, Runnable::run);
        final var transfer = new MoneyTransfer(new AccountId("from"), new AccountId("to"), Money.of(50, "EUR"));

        executor.execute(transfer);

        assertEquals(Money.of(50, "EUR"), accounts.get(new AccountId("from")).getBalance());
        assertEquals(Money.of(150, "EUR"), accounts.get(new AccountId("to")).getBalance());
    }

    /**
     * Quick and dirty way to verify that the concurrent executor is up to the task of handling
     * concurrent money transfers between the same accounts: manually run this test.
     */
    @Test
    @Disabled
    void concurrentExecutorShouldWorkWhenUsedConcurrently() {
        final var transactionCount = 1000;
        final var transactionExecutingThreadPool = Executors.newFixedThreadPool(4);
        final var accounts = accountMapOf(
                Account.of("from", "EUR", transactionCount * 100),
                Account.of("to", "EUR", 0)
        );

        final var transferOneEuro = new MoneyTransfer(new AccountId("from"), new AccountId("to"), Money.of(1, "EUR"));
        final var transactionExecutor = new ConcurrentTransactionExecutor(accounts::get, transactionExecutingThreadPool);

        for (int i = 0; i < transactionCount; ++i) {
            transactionExecutor.execute(transferOneEuro);
        }

        try {
            transactionExecutingThreadPool.shutdown();
            transactionExecutingThreadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(transactionCount, accounts.get(new AccountId("to")).getBalance().getNumber().intValueExact());
    }

    private static Map<AccountId, Account> accountMapOf(Account... accounts) {
        var map = new HashMap<AccountId, Account>();
        for (var account : accounts) {
            map.put(account.getId(), account);
        }

        return map;
    }
}
