package com.jannis.assignment.revolut.domain.transaction.execution;

import com.jannis.assignment.revolut.domain.account.AccountId;
import com.jannis.assignment.revolut.domain.account.AccountResolver;
import com.jannis.assignment.revolut.domain.transaction.TransactionIntent;

import java.util.concurrent.Executor;

public class SimplestPossibleTransactionExecutor implements TransactionExecutor {
    private final Executor executor;
    private final TransactionExecutionContext context;

    public SimplestPossibleTransactionExecutor(AccountResolver accountResolver, Executor executor) {
        this.executor = executor;
        this.context = new TransactionExecutionContext() {
            @Override
            public AccountResolver getAccountResolver() {
                return accountResolver;
            }

            @Override
            public boolean acquireTransactionLock(AccountId accountId) {
                return true;
            }

            @Override
            public boolean releaseTransactionLock(AccountId accountId) {
                return true;
            }
        };
    }

    @Override
    public void execute(TransactionIntent transactionIntent) {
        this.executor.execute(() -> transactionIntent.execute(context));
    }
}
