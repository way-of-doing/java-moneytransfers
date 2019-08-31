package com.jannis.assignment.revolut.domain.transaction.execution;

import com.jannis.assignment.revolut.domain.transaction.TransactionIntent;

@FunctionalInterface
public interface TransactionExecutor {
    void execute(TransactionIntent transactionIntent);
}
