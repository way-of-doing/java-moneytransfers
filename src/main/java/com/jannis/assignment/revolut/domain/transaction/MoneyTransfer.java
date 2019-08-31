package com.jannis.assignment.revolut.domain.transaction;

import com.jannis.assignment.revolut.domain.account.AccountId;
import com.jannis.assignment.revolut.domain.transaction.execution.TransactionExecutionContext;

import javax.money.MonetaryAmount;
import java.util.Objects;
import java.util.stream.Stream;

public final class MoneyTransfer implements TransactionIntent {
    private final AccountId sourceId;
    private final AccountId destinationId;
    private final MonetaryAmount amount;

    public MoneyTransfer(AccountId sourceId, AccountId destinationId, MonetaryAmount amount) {
        this.sourceId = Objects.requireNonNull(sourceId, "Transaction source account id cannot be null");
        this.destinationId = Objects.requireNonNull(destinationId, "Transaction destination id account cannot be null");
        this.amount = Objects.requireNonNull(amount, "Transaction amount cannot be null");
    }

    public AccountId getSourceId() {
        return sourceId;
    }

    public AccountId getDestinationId() {
        return destinationId;
    }

    public MonetaryAmount getAmount() {
        return amount;
    }

    @Override
    public Stream<AccountId> getInvolvedAccounts() {
        return Stream.of(this.sourceId, this.destinationId);
    }

    @Override
    public void execute(TransactionExecutionContext context) {
        final var source = context.getAccountResolver().resolve(this.sourceId);
        final var destination = context.getAccountResolver().resolve(this.destinationId);

        if (source == null) {
            throw new TransactionException("Could not resolve source account id");
        } else if (destination == null) {
            throw new TransactionException("Could not resolve destination account id");
        } else if (!source.getBalance().getCurrency().equals(destination.getBalance().getCurrency())) {
            throw new TransactionException("Source and destination denominations are not the same");
        } else if (!source.getBalance().getCurrency().equals(this.amount.getCurrency())) {
            throw new TransactionException("Source and amount denominations are not the same");
        } else if (source.getBalance().isLessThan(this.amount)) {
            throw new TransactionException("Not enough money in the source account");
        }

        source.setBalance(source.getBalance().subtract(this.amount));
        destination.setBalance(destination.getBalance().add(this.amount));
    }
}
