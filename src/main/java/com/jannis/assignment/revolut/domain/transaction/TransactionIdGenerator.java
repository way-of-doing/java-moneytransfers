package com.jannis.assignment.revolut.domain.transaction;

import java.util.concurrent.atomic.AtomicLong;

public final class TransactionIdGenerator {
    private final AtomicLong currentId = new AtomicLong(1);

    public TransactionId getNextId() {
        return new TransactionId(String.valueOf(currentId.getAndIncrement()));
    }
}
