package com.jannis.assignment.revolut;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jannis.assignment.revolut.api.ApiServer;
import com.jannis.assignment.revolut.api.util.AccountSerializer;
import com.jannis.assignment.revolut.api.util.HttpStatusCodes;
import com.jannis.assignment.revolut.api.util.MoneyTransferSerializer;
import com.jannis.assignment.revolut.domain.account.Account;
import com.jannis.assignment.revolut.domain.transaction.MoneyTransfer;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.*;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    private static class RequestResult {
        final int responseCode;
        final String responseBody;

        RequestResult(int responseCode, String responseBody) {
            this.responseBody = responseBody;
            this.responseCode = responseCode;
        }
    }

    private static Gson gson;

    @BeforeAll
    static void initJsonParser() {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(Account.class, new AccountSerializer())
                .registerTypeAdapter(MoneyTransfer.class, new MoneyTransferSerializer())
                .create();
    }

    @BeforeEach
    void initApiServer() throws ExecutionException, InterruptedException {
        ApiServer.start().get();
    }

    @AfterEach
    void shutdownApiServer() {
        ApiServer.stop();
    }

    @Test
    void creatingAnAccountWorks() {
        final var account = Account.of("foo", "EUR", 123.45);

        final var postAccountResponse = makeRequestAndGetResponse(
                "POST",
                "account/",
                getRequestBodyToCreateAccount(account)
        );

        assertEquals(HttpStatusCodes.CREATED, postAccountResponse.responseCode);

        final var getAccountResponse = makeRequestAndGetResponse("GET", String.format("account/%s/", account.getId().getValue()));

        assertEquals(HttpStatusCodes.OK, getAccountResponse.responseCode);

        final var retrievedAccount = deserializeAccount(getAccountResponse);

        assertEquals(account.getId(), retrievedAccount.getId());
        assertEquals(account.getBalance(), retrievedAccount.getBalance());
    }

    @Test
    void updatingAnAccountWorks() {
        final var initialAccount = Account.of("foo", "EUR", 123.45);
        final var updatedAccount = Account.of("foo", "USD", 567.89);

        final var postAccountResponse = makeRequestAndGetResponse(
                "POST",
                "account/",
                getRequestBodyToCreateAccount(initialAccount)
        );

        assertEquals(HttpStatusCodes.CREATED, postAccountResponse.responseCode);

        final var putAccountResponse = makeRequestAndGetResponse(
                "PUT",
                String.format("account/%s/", initialAccount.getId().getValue()),
                getRequestBodyToCreateAccount(updatedAccount)
        );

        assertEquals(HttpStatusCodes.OK, putAccountResponse.responseCode);

        final var getAccountResponse = makeRequestAndGetResponse("GET", String.format("account/%s/", updatedAccount.getId().getValue()));

        assertEquals(HttpStatusCodes.OK, getAccountResponse.responseCode);

        final var retrievedAccount = deserializeAccount(getAccountResponse);

        assertEquals(updatedAccount.getId(), retrievedAccount.getId());
        assertEquals(updatedAccount.getBalance(), retrievedAccount.getBalance());
    }

    @Test
    void deletingAnAccountWorks() {
        final var accountId = "foo";
        final var account = Account.of(accountId, "EUR", 123.45);

        assertEquals(
                HttpStatusCodes.NOT_FOUND,
                makeRequestAndGetResponse("GET", String.format("account/%s/", accountId)).responseCode
        );

        assertEquals(
                HttpStatusCodes.CREATED,
                makeRequestAndGetResponse("POST", "account/", getRequestBodyToCreateAccount(account)).responseCode
        );

        assertEquals(
                HttpStatusCodes.OK,
                makeRequestAndGetResponse("DELETE", String.format("account/%s/", accountId)).responseCode
        );

        assertEquals(
                HttpStatusCodes.NOT_FOUND,
                makeRequestAndGetResponse("GET", String.format("account/%s/", accountId)).responseCode
        );
    }

    @Test
    void moneyTransferWorksWhenFundsAvailable() {
        final var sourceAccount = Account.of("source", "EUR", 1000);
        final var destinationAccount = Account.of("destination", "EUR", 0);
        final var transfer = new MoneyTransfer(sourceAccount.getId(), destinationAccount.getId(), Money.of(123.45d, "EUR"));

        makeRequestAndGetResponse("POST", "account/", getRequestBodyToCreateAccount(sourceAccount));
        makeRequestAndGetResponse("POST", "account/", getRequestBodyToCreateAccount(destinationAccount));

        assertEquals(
                HttpStatusCodes.ACCEPTED,
                makeRequestAndGetResponse("POST", "moneytransfer/", getRequestBodyToCreateTransfer(transfer)).responseCode
        );

        try {
            Thread.sleep(500); // allow some time for the transaction to be processed
        } catch (InterruptedException ignored) {}

        var getSourceResponse = makeRequestAndGetResponse("GET", String.format("account/%s/", sourceAccount.getId().getValue()));
        var getDestinationResponse = makeRequestAndGetResponse("GET", String.format("account/%s/", destinationAccount.getId().getValue()));

        assertEquals(HttpStatusCodes.OK, getSourceResponse.responseCode);
        assertEquals(HttpStatusCodes.OK, getDestinationResponse.responseCode);

        final var retrievedSource = deserializeAccount(getSourceResponse);
        assertEquals(sourceAccount.getId(), retrievedSource.getId());
        assertEquals(sourceAccount.getBalance().subtract(transfer.getAmount()), retrievedSource.getBalance());

        final var retrievedDestination = deserializeAccount(getDestinationResponse);
        assertEquals(destinationAccount.getId(), retrievedDestination.getId());
        assertEquals(destinationAccount.getBalance().add(transfer.getAmount()), retrievedDestination.getBalance());
    }

    private String getRequestBodyToCreateAccount(Account account) {
        final var template = "{ 'id': '%s', 'balance': { 'currency': '%s', 'value': '%s' } }";

        return String.format(
                template.replace('\'', '"'),
                account.getId().getValue(),
                account.getBalance().getCurrency().getCurrencyCode(),
                account.getBalance().getNumber()
        );
    }

    private String getRequestBodyToCreateTransfer(MoneyTransfer transfer) {
        final var template = "{ 'from': '%s', 'to': '%s', 'value': { 'currency': '%s', 'amount': '%s' } }";

        return String.format(
                template.replace('\'', '"'),
                transfer.getSourceId().getValue(),
                transfer.getDestinationId().getValue(),
                transfer.getAmount().getCurrency().getCurrencyCode(),
                transfer.getAmount().getNumber()
        );
    }

    private RequestResult makeRequestAndGetResponse(String method, String path) {
        return makeRequestAndGetResponse(method, path, null);
    }

    private RequestResult makeRequestAndGetResponse(String method, String path, String requestBody) {
        try {
            final var url = new URL("http://localhost:8888/" + path);
            final var connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);

            if (requestBody != null) {
                connection.setDoOutput(true);
                final var outputStreamWriter = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
                outputStreamWriter.write(requestBody.replace('\'', '"'));
                outputStreamWriter.flush();
                outputStreamWriter.close();
            }

            final var responseCode = connection.getResponseCode();
            final var inputStream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            final var responseBodyScanner = new Scanner(inputStream).useDelimiter("\\A");
            final var responseBody = responseBodyScanner.hasNext() ? responseBodyScanner.next() : "";
            connection.disconnect();

            return new RequestResult(responseCode, responseBody);
        }
        catch (Exception e) {
            System.err.println(e.toString());
            return new RequestResult(-1, "");
        }
    }

    private Account deserializeAccount(RequestResult result) {
        return gson.fromJson(result.responseBody, Account.class);
    }
}
