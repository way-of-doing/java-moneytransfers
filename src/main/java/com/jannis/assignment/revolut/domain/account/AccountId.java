package com.jannis.assignment.revolut.domain.account;

import java.util.Objects;

public final class AccountId {
    private final String id;

    public AccountId(String id) {
        this.id = Objects.requireNonNull(id, "Account id cannot be null");
        if (this.id.isEmpty()) {
            throw new IllegalArgumentException("Account id cannot be empty");
        }
    }

    public String getValue() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountId that = (AccountId) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AccountId{" + id + "}";
    }
}
