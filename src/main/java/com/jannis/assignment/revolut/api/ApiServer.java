package com.jannis.assignment.revolut.api;

import com.google.gson.GsonBuilder;
import com.jannis.assignment.revolut.api.util.AccountSerializer;
import com.jannis.assignment.revolut.api.util.ExceptionHandler;
import com.jannis.assignment.revolut.api.util.MoneyTransferSerializer;
import com.jannis.assignment.revolut.domain.account.Account;
import com.jannis.assignment.revolut.domain.transaction.MoneyTransfer;
import spark.Spark;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static spark.Spark.*;
import static spark.Spark.post;

final public class ApiServer {
    private static boolean hasStarted = false;
    private static CompletableFuture<Boolean> startFuture = new CompletableFuture<>();
    private static ThreadPoolExecutor modelThreadPool;

    public static boolean start() {
        if (hasStarted) {
            return false;
        }

        hasStarted = true;

        // Setup controller and dependencies
        final var gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(Account.class, new AccountSerializer())
                .registerTypeAdapter(MoneyTransfer.class, new MoneyTransferSerializer())
                .create();

        modelThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        final var model = new ApiModel(modelThreadPool);
        final var controller = new ApiController(model, gson);

        // Setup HTTP server globals
        final int listeningPort = 8888;
        final int maxThreads = 4;
        final int minThreads = 2;
        final int timeOutMillis = 30000;

        initExceptionHandler(startFuture::completeExceptionally);

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

        new Thread(() -> { awaitInitialization(); startFuture.complete(true); }).start();

        try {
            return startFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Could not start API server", e);
        }
    }

    public static void stop() {
        try {
            if (hasStarted && startFuture.get()) {
                modelThreadPool.shutdown();
                Spark.stop();
                awaitStop();
            }
        } catch (InterruptedException | ExecutionException ignored) {}
    }
}
