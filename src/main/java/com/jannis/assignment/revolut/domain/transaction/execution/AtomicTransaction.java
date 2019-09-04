package com.jannis.assignment.revolut.domain.transaction.execution;

import com.jannis.assignment.revolut.domain.account.AccountId;
import com.jannis.assignment.revolut.domain.transaction.TransactionIntent;

import java.util.function.Consumer;
import java.util.stream.Stream;

final class AtomicTransaction implements TransactionIntent {
    private final TransactionIntent inner;

    AtomicTransaction(TransactionIntent transactionIntent) {
        this.inner = transactionIntent;
    }

    @Override
    public Stream<AccountId> getInvolvedAccounts() {
        return inner.getInvolvedAccounts();
    }

    @Override
    public void execute(TransactionExecutionContext context) throws TransactionLockingFailedException {
        var involvedAccounts = this.inner.getInvolvedAccounts().toArray(AccountId[]::new);
        makeExecutionAtomicByLocking(this.inner::execute, involvedAccounts).accept(context);
    }

    private static Consumer<TransactionExecutionContext>
    makeExecutionAtomicByLocking(Consumer<TransactionExecutionContext> businessLogic, AccountId[] accounts) {
        return context -> {
            try {
                for (var accountId : accounts) {
                    if (!context.acquireTransactionLock(accountId)) {
                        throw new TransactionLockingFailedException();
                    }
                }

                businessLogic.accept(context);
            } finally {
                for (var accountId : accounts) {
                    context.releaseTransactionLock(accountId);
                }
            }
        };
    }
}
