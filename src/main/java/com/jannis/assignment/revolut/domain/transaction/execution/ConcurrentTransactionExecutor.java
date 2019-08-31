package com.jannis.assignment.revolut.domain.transaction.execution;

import com.jannis.assignment.revolut.domain.account.AccountId;
import com.jannis.assignment.revolut.domain.account.AccountResolver;
import com.jannis.assignment.revolut.domain.transaction.TransactionIntent;
import com.jannis.assignment.revolut.domain.transaction.TransactionId;
import com.jannis.assignment.revolut.domain.transaction.TransactionIdGenerator;

import java.util.concurrent.*;

public class ConcurrentTransactionExecutor implements TransactionExecutor {
    private final TransactionIdGenerator transactionIdGenerator = new TransactionIdGenerator();
    private final ConcurrentMap<AccountId, TransactionId> accountAtomicityAssociation = new ConcurrentHashMap<>();
    private final AccountResolver accountResolver;
    private final Executor executor;

    public ConcurrentTransactionExecutor(AccountResolver accountResolver, Executor executor) {
        this.accountResolver = accountResolver;
        this.executor = executor;
    }

    @Override
    public void execute(TransactionIntent transactionIntent) {
        final var context = this.createTransactionContext();
        final var atomicTransaction = new AtomicTransaction(transactionIntent);

        Runnable executeAsSoonAsTransactionLockAcquired = () -> {
            while (true) {
                try {
                    atomicTransaction.execute(context);
                    break;
                } catch (TransactionLockingFailedException ignored) {}
            }
        };

        this.executor.execute(executeAsSoonAsTransactionLockAcquired);
    }

    private TransactionExecutionContext createTransactionContext() {
        final var transactionId = this.transactionIdGenerator.getNextId();
        final var accountAtomicityAssociation = this.accountAtomicityAssociation;
        final var accountResolver = this.accountResolver;

        return new TransactionExecutionContext() {
            @Override
            public AccountResolver getAccountResolver() {
                return accountResolver;
            }

            @Override
            public boolean acquireTransactionLock(AccountId accountId) {
                return accountAtomicityAssociation.putIfAbsent(accountId, transactionId) == null;
            }

            @Override
            public boolean releaseTransactionLock(AccountId accountId) {
                return accountAtomicityAssociation.remove(accountId, transactionId);
            }
        };
    }
}
