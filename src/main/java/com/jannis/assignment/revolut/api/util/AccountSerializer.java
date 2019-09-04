package com.jannis.assignment.revolut.api.util;

import com.google.gson.*;
import com.jannis.assignment.revolut.domain.account.Account;

import java.lang.reflect.Type;

public class AccountSerializer implements JsonSerializer<Account>, JsonDeserializer<Account> {
    @Override
    public JsonElement serialize(Account account, Type accountType, JsonSerializationContext context) {
        var serialized = new JsonObject();
        var balance = new JsonObject();
        serialized.add("id", new JsonPrimitive(account.getId().getValue()));
        serialized.add("balance", balance);
        balance.addProperty("value", account.getBalance().getNumber());
        balance.addProperty("currency", account.getBalance().getCurrency().getCurrencyCode());
        return serialized;
    }

    @Override
    public Account deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        final var body = element.getAsJsonObject();
        final var id = body.get("id");
        final var balance = body.getAsJsonObject("balance");

        if (id == null || !id.isJsonPrimitive()) {
            throw new JsonParseException("account 'id' element missing or not a string");
        } else if (balance == null) {
            throw new JsonParseException("account without 'balance' element");
        }

        final var balanceValue = balance.get("value");
        final var balanceCurrency = balance.get("currency");

        if (balanceValue == null || !balanceValue.isJsonPrimitive()) {
            throw new JsonParseException("balance 'value' element missing or not a primitive");
        } else if (balanceCurrency == null || !balanceCurrency.isJsonPrimitive()) {
            throw new JsonParseException("balance 'currency' element missing or not a primitive");
        }

        return Account.of(id.getAsString(), balanceCurrency.getAsString(), balanceValue.getAsNumber());
    }
}
