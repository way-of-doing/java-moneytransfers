package com.jannis.assignment.revolut.api.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import spark.Request;
import spark.Response;

import javax.money.UnknownCurrencyException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;

import static spark.Spark.exception;

public final class ExceptionHandler {
    private static Gson Gson;

    public static void initialize(Gson gson) {
        ExceptionHandler.Gson = gson;

        exception(UnknownCurrencyException.class, wrap(ExceptionHandler::unknownCurrency));
        exception(JsonDeserializationException.class, wrap(ExceptionHandler::jsonDeserialization));
        exception(JsonSyntaxException.class, wrap(ExceptionHandler::jsonParse));
        exception(NumberFormatException.class, wrap(ExceptionHandler::numberFormat));
        exception(Exception.class, ExceptionHandler::debug);
    }

    private static void debug(Exception e, Request request, Response response)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        response.status(HttpStatusCodes.INTERNAL_SERVER_ERROR);
        response.header("Content-Type", "text/plain");
        response.body(String.format("%s\n\n%s", e.toString(), sw.toString()));
    }

    private static String numberFormat(NumberFormatException e) {
        return e.getMessage();
    }

    private static String jsonDeserialization(JsonDeserializationException e) {
        return e.getMessage();
    }

    private static String unknownCurrency(UnknownCurrencyException e) {
        return String.format("unknown currency code: '%s'", e.getCurrencyCode());
    }

    private static String jsonParse(JsonParseException e) {
        return "invalid JSON input";
    }

    private static <T extends Exception> spark.ExceptionHandler<T> wrap(Function<T, String> handler) {
        return wrap(handler, HttpStatusCodes.BAD_REQUEST);
    }

    private static <T extends Exception> spark.ExceptionHandler<T> wrap(Function<T, String> handler, int statusCode) {
        return (T e, Request request, Response response) -> {
            var body = new JsonObject();
            body.addProperty("error", handler.apply(e));
            response.status(statusCode);
            response.header("Content-Type", "application/json");
            response.body(Gson.toJson(body));
        };
    }
}
