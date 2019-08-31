package com.jannis.assignment.revolut.domain.transaction.execution;

import com.jannis.assignment.revolut.domain.account.Account;
import com.jannis.assignment.revolut.domain.account.AccountId;
import com.jannis.assignment.revolut.domain.transaction.MoneyTransfer;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SimplestPossibleTransactionExecutorTest {
    /**
     * Quick and dirty way to verify that the simple executor makes a mess of things when
     * money transfers between the same accounts are running concurrently: manually run this test.
     */
    @Test
    @Disabled
    void simpleExecutorShouldFailWhenUsedConcurrently() {
        final var transactionCount = 1000;
        final var transactionExecutingThreadPool = Executors.newFixedThreadPool(4);
        final var accounts = accountMapOf(
                Account.of("from", "EUR", transactionCount * 100),
                Account.of("to", "EUR", 0)
        );

        final var transferOneEuro = new MoneyTransfer(new AccountId("from"), new AccountId("to"), Money.of(1, "EUR"));
        final var transactionExecutor = new SimplestPossibleTransactionExecutor(accounts::get, transactionExecutingThreadPool);

        for (int i = 0; i < transactionCount; ++i) {
            transactionExecutor.execute(transferOneEuro);
        }

        try {
            transactionExecutingThreadPool.shutdown();
            transactionExecutingThreadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertNotEquals(transactionCount, accounts.get(new AccountId("to")).getBalance().getNumber().intValueExact());
    }

    private static Map<AccountId, Account> accountMapOf(Account... accounts) {
        var map = new HashMap<AccountId, Account>();
        for (var account : accounts) {
            map.put(account.getId(), account);
        }

        return map;
    }
}