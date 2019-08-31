package com.jannis.assignment.revolut.api;

import com.google.gson.GsonBuilder;
import com.jannis.assignment.revolut.api.util.AccountSerializer;
import com.jannis.assignment.revolut.api.util.ExceptionHandler;
import com.jannis.assignment.revolut.api.util.MoneyTransferSerializer;
import com.jannis.assignment.revolut.domain.account.Account;
import com.jannis.assignment.revolut.domain.transaction.MoneyTransfer;
import spark.Spark;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static spark.Spark.*;
import static spark.Spark.post;

final public class ApiServer {
    public static Future<Boolean> start() {
        // Bit awkward signalling mechanism for initialization success
        var future = new CompletableFuture<Boolean>();

        // Setup controller and dependencies
        var gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(Account.class, new AccountSerializer())
                .registerTypeAdapter(MoneyTransfer.class, new MoneyTransferSerializer())
                .create();
        var businessLogicExecutor = Executors.newFixedThreadPool(4);
        var model = new ApiModel(businessLogicExecutor);
        var controller = new ApiController(model, gson);

        // Setup HTTP server globals
        final int listeningPort = 8888;
        final int maxThreads = 4;
        final int minThreads = 2;
        final int timeOutMillis = 30000;

        initExceptionHandler(future::completeExceptionally);

        threadPool(maxThreads, minThreads, timeOutMillis);
        port(listeningPort);

        ExceptionHandler.initialize(gson);

        // Some niceties
        notFound(((request, response) -> ""));
        before("*", ((request, response) -> {
            // auto-add trailing slashes
            if (!request.pathInfo().endsWith("/")) {
                response.redirect(request.pathInfo() + "/");
            }

            response.type("application/json");
        }));

        // Setup routes
        path("/account", () -> {
            get("/:id/", controller::getAccount);
            put("/:id/", controller::putAccount);
            delete("/:id/", controller::deleteAccount);
            post("/", controller::postAccount);
        });

        post("/moneytransfer/", controller::postMoneyTransfer);

        new Thread(() -> { awaitInitialization(); future.complete(true); }).start();

        return future;
    }

    public static void stop() {
        Spark.stop();
        awaitStop();
    }
}
