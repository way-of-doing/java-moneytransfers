package com.jannis.assignment.revolut.domain.account;

import static org.junit.jupiter.api.Assertions.*;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

class AccountTest
{
    @Test
    void accountCannotBeCreatedWithNullId() {
        assertThrows(NullPointerException.class, () -> Account.of(null, "EUR", 0));
    }

    @Test
    void accountCannotBeCreatedWithNullBalance() {
        assertThrows(NullPointerException.class, () -> new Account(new AccountId("foo"), null));
    }

    @Test
    void accountDoesNotAllowChangingDenomination() {
        final var account = Account.of("foo", "EUR", 0);

        assertThrows(IllegalArgumentException.class, () -> account.setBalance(Money.of(0, "USD")));
    }
}
