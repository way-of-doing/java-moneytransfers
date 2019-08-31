package com.jannis.assignment.revolut.domain.account;

import org.javamoney.moneta.Money;

import javax.money.MonetaryAmount;
import java.util.Objects;

public final class Account {
    private final AccountId id;
    private MonetaryAmount balance;

    Account(AccountId id, MonetaryAmount balance) {
        this.id = Objects.requireNonNull(id, "Account id cannot be null");
        this.balance = Objects.requireNonNull(balance, "Account balance cannot be null");
    }

    public static Account of(String accountId, String currencyCode, Number balance) {
        return new Account(new AccountId(accountId), Money.of(balance, currencyCode));
    }

    public AccountId getId() {
        return this.id;
    }

    public MonetaryAmount getBalance() {
        return this.balance;
    }

    public void setBalance(MonetaryAmount newBalance) {
        if (!this.balance.getCurrency().equals(newBalance.getCurrency())) {
            throw new IllegalArgumentException("Changing the denomination of an account after creation is not allowed");
        }

        this.balance = newBalance;
    }
}
