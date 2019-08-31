package com.jannis.assignment.revolut.api;

import com.jannis.assignment.revolut.domain.account.Account;
import com.jannis.assignment.revolut.domain.account.AccountId;
import com.jannis.assignment.revolut.domain.transaction.execution.ConcurrentTransactionExecutor;
import com.jannis.assignment.revolut.domain.transaction.MoneyTransfer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

public class ApiModel {
    private final ConcurrentMap<AccountId, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentTransactionExecutor executor;

    public ApiModel(Executor executor) {
        this.executor = new ConcurrentTransactionExecutor(this.accounts::get, executor);
    }

    boolean createNewAccount(Account account) {
        return this.accounts.putIfAbsent(account.getId(), account) == null;
    }

    Account getAccount(String id) {
        return this.accounts.get(new AccountId(id));
    }

    Account deleteAccount(String id) {
        return this.accounts.remove(new AccountId(id));
    }

    void createOrReplaceAccount(Account account) {
        this.accounts.put(account.getId(), account);
    }

    void moneyTransfer(MoneyTransfer transfer) {
        this.executor.execute(transfer);
    }
}
