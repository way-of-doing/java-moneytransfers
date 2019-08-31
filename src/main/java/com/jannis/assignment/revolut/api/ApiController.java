package com.jannis.assignment.revolut.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jannis.assignment.revolut.api.util.HttpStatusCodes;
import com.jannis.assignment.revolut.domain.account.Account;
import com.jannis.assignment.revolut.domain.transaction.MoneyTransfer;
import spark.Request;
import spark.Response;

public class ApiController {
    private final ApiModel model;
    private final Gson gson;

    private final static String EMPTY_RESPONSE = "";

    public ApiController(ApiModel model, Gson gson) {
        this.model = model;
        this.gson = gson;
    }

    public Object getAccount(Request request, Response response) {
        var account = this.model.getAccount(request.params("id"));
        if (account != null) {
            response.status(HttpStatusCodes.OK);
            return this.gson.toJson(account);
        } else {
            response.status(HttpStatusCodes.NOT_FOUND);
            return EMPTY_RESPONSE;
        }
    }

    public Object deleteAccount(Request request, Response response) {
        var account = this.model.deleteAccount(request.params("id"));
        if (account != null) {
            response.status(HttpStatusCodes.OK);
            return this.gson.toJson(account);
        } else {
            response.status(HttpStatusCodes.NOT_FOUND);
            return EMPTY_RESPONSE;
        }
    }

    public Object putAccount(Request request, Response response) {
        var account = this.gson.fromJson(request.body(), Account.class);

        if (account.getId().getValue().equals(request.params("id"))) {
            this.model.createOrReplaceAccount(account);
            response.status(HttpStatusCodes.OK);
            return this.gson.toJson(account);
        } else {
            response.status(HttpStatusCodes.BAD_REQUEST);
            return simpleResponse("error", "id in the URL does not match id in the payload");
        }
    }

    public Object postAccount(Request request, Response response) {
        var account = this.gson.fromJson(request.body(), Account.class);
        if (this.model.createNewAccount(account)) {
            response.status(HttpStatusCodes.CREATED);
            return this.gson.toJson(account);
        } else {
            response.status(HttpStatusCodes.CONFLICT);
            return simpleResponse("error", "an account with this id already exists");
        }
    }

    public Object postMoneyTransfer(Request request, Response response) {
        var transfer = this.gson.fromJson(request.body(), MoneyTransfer.class);
        this.model.moneyTransfer(transfer);
        response.status(HttpStatusCodes.ACCEPTED);
        return EMPTY_RESPONSE;
    }

    private String simpleResponse(String title, String message) {
        var object = new JsonObject();
        object.addProperty(title, message);
        return this.gson.toJson(object);
    }
}
