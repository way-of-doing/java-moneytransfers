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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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
    void initApiServer() {
        ApiServer.start();
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
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(1))
                    .executor(Runnable::run)
                    .build();

            final var bodyPublisher = requestBody == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8888/" + path))
                    .timeout(Duration.ofSeconds(1))
                    .header("Content-Type", "application/json")
                    .method(method, bodyPublisher)
                    .build();

            final var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return new RequestResult(response.statusCode(), response.body());
        }
        catch (IOException | InterruptedException e) {
            System.err.println(e.toString());
            return new RequestResult(-1, "");
        }
    }

    private Account deserializeAccount(RequestResult result) {
        return gson.fromJson(result.responseBody, Account.class);
    }
}
