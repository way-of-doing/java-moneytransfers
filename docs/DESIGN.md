## Design Goals

The backend should work in a manner that affords

- Concurrency safety (obviously needed even in toy-sized production systems)
- Reasonable scaling options (a single giant lock around any transaction would work correctly, but such a solution is not always applicable to practical systems)
- Extensibility (there should be a clear path to consider when discussing additional features such as transaction queuing, delayed execution, etc)

Apart from the above specific points, standard good engineering practices were of course kept in consideration.

## Core Concepts

A `TransactionExecutor` is an entity that facilitates execution of a `TransactionIntent` (such as a money transfer). A transaction intent (rather than plain "transaction", which is arguably a term better suited to a the result of successfully completing the execution of an intent) is an abstract concept modelling a process that operates on one or more `Account`s. Accounts involved in the execution of each transaction might or might not have their state changed as a result of the transaction. Transaction intents encode the business logic required to execute the transaction, and are provided the services of a `TransactionExecutionContext` by the executor in order to facilitate the execution.

It's important to note that a `TransactionIntent` should always be instantiated in a manner that does not involve interaction with current system state at the time of instantiation; for example, an intent should never be instantiated with knowledge of the *state* of an account but only with knowledge its *identity* (modelled as an `AccountId`) instead.

This design leaves many details dependent on the concrete implementation of `TransactionExecutor`, where policies such as queuing and retrying would be implemented in terms of strategies the executor is configured to follow, and `TransactionExecutionContext`, which can provide a variable number of services to the business logic of an intent. This leaves each `TransactionIntent` free to focus on *what* it wants to do instead of *how* it wants to do it; since it depends on the execution context for any contact with the rest of the system, and the context is provided by the executor, the executor is free to construct any kind of real or simulated environment that the transaction will operate in and therefore is always in full control.

## Implementation

#### `ConcurrentTransactionExecutor`

The production-level executor. It depends on an `AccountResolver` (lightweight functional interface allowing clients to plug in arbitrary "account data storage") and a Java `Executor` (another functional interface allowing clients to plug in arbitrary transaction execution strategies).

This is where each transaction intent is guaranteed to be executed atomically. The concept of making arbitrary transactions atomic is implemented thus:

- Transaction intents must never capture system state outside the duration of their `execute` method.
- Transaction intents must report the set of accounts being operated on in terms of their ids.
- The executor generates a guaranteed unique transaction id for every transaction it attempts to execute; it uses this id as a value to be associated with each account id the transaction will operate on, effectively saying "account X state is currently locked because of transaction Y that operates on it being executed".
- In order to achieve this in a concurrency-safe manner that also allows multiple unrelated transactions to execute in parallel, a `ConcurrentHashMap` from account ids to transaction ids is used.

#### `AtomicTransaction`

This is a wrapper around a `TransactionIntent` that encodes the lock-execute-unlock logic. Locking is expected to fail in a contention-rich environment; if it does, a locking exception is thrown and the transaction executor is responsible for deciding how to deal with it (in the above implementation, by simply retrying until the lock is acquired).