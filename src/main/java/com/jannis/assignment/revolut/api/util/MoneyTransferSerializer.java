package com.jannis.assignment.revolut.api.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.jannis.assignment.revolut.domain.account.AccountId;
import com.jannis.assignment.revolut.domain.transaction.MoneyTransfer;
import org.javamoney.moneta.Money;

import java.lang.reflect.Type;

public class MoneyTransferSerializer implements JsonDeserializer<MoneyTransfer> {
    @Override
    public MoneyTransfer deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        final var body = element.getAsJsonObject();
        final var from = body.get("from");
        final var to = body.get("to");
        final var value = body.getAsJsonObject("value");

        if (from == null || !from.isJsonPrimitive()) {
            throw new JsonParseException("money transfer 'from' element missing or not a string");
        } else if (to == null || !to.isJsonPrimitive()) {
            throw new JsonParseException("money transfer 'to' element missing or not a string");
        } else if (value == null || !value.isJsonObject()) {
            throw new JsonParseException("money transfer 'value' element missing or not an object");
        }

        var sourceId = new AccountId(from.getAsString());
        var destinationId = new AccountId(to.getAsString());

        var valueCurrency = value.get("currency");
        var valueAmount = value.get("amount");

        if (valueCurrency == null || !valueCurrency.isJsonPrimitive()) {
            throw new JsonParseException("money transfer value 'currency' element missing or not a primitive");
        } else if (valueAmount == null || !valueAmount.isJsonPrimitive()) {
            throw new JsonParseException("money transfer value 'amount' element missing or not a primitive");
        }

        return new MoneyTransfer(sourceId, destinationId, Money.of(valueAmount.getAsNumber(), valueCurrency.getAsString()));
    }
}
