package com.jannis.assignment.revolut.domain.transaction;

import java.util.Objects;

public final class TransactionId {
    private final String id;

    TransactionId(String id) {
        this.id = Objects.requireNonNull(id, "Transaction id cannot be null");
        if (this.id.isEmpty()) {
            throw new IllegalArgumentException("Transaction id cannot be empty");
        }
    }

    String getValue() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionId that = (TransactionId) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TransactionId{" + id + "}";
    }
}
