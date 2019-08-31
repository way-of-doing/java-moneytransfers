package com.jannis.assignment.revolut.domain.account;

@FunctionalInterface
public interface AccountResolver {
    Account resolve(AccountId id);
}
