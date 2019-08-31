package com.jannis.assignment.revolut.domain.transaction;

import com.jannis.assignment.revolut.domain.account.AccountId;
import com.jannis.assignment.revolut.domain.transaction.execution.TransactionExecutionContext;

import java.util.stream.Stream;

public interface TransactionIntent {
    Stream<AccountId> getInvolvedAccounts();
    void execute(TransactionExecutionContext context);
}
