package com.jannis.assignment.revolut.domain.transaction.execution;

import com.jannis.assignment.revolut.domain.account.AccountId;
import com.jannis.assignment.revolut.domain.account.AccountResolver;

public interface TransactionExecutionContext {
    AccountResolver getAccountResolver();
    boolean acquireTransactionLock(AccountId accountId);
    boolean releaseTransactionLock(AccountId accountId);
}
