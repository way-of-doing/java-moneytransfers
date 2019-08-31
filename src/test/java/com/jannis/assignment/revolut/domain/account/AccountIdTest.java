package com.jannis.assignment.revolut.domain.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountIdTest {
    @Test
    void accountIdCannotBeCreatedWithNullId() {
        assertThrows(NullPointerException.class, () -> new AccountId(null));
    }

    @Test
    void accountIdCannotBeCreatedWithEmptyId() {
        assertThrows(IllegalArgumentException.class, () -> new AccountId(""));
    }

    @Test
    void accountIdEqualsWorksInTermsOfValue() {
        assertEquals(new AccountId("f" + "oo"), new AccountId("fo" + "o"));
    }
}
