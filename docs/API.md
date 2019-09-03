## API Overview
The API provides endpoints to [create](#create-account), [update](#update-account), [retrieve](#get-account),
and [delete](#delete-account) accounts; and also to initiate [money transfers](#money-transfer) among them.

There are no authentication facilities. Response codes to be expected for correctly formed requests are in the 200 and
400 ranges.

Note that all endpoint URLs end with a slash, but automatic redirection with HTTP code 302 and a `Location` header is
performed if a request URL does not end with a slash.

## API Endpoints

### Get Account

```http
GET /account/:id/
```

Retrieves information for account with id `:id`.

###### Responses

| Status Code | Description                                                  |
| ----------- | ------------------------------------------------------------ |
| 200         | Account with the specified id exists. Response body includes account information. |
| 404         | Account does not exist.                                      |

###### Example

Request:

```bash
curl --location --request GET "http://localhost:8888/account/foo/"
```

Response:

```json
{
  "id": "foo",
  "balance": {
    "value": 1000,
    "currency": "EUR"
  }
}
```

### Create Account

```http
POST /account/
```

Creates a new account. Request body must include account information in JSON format.

###### Responses

| Status Code | Description                                                  |
| ----------- | ------------------------------------------------------------ |
| 201         | Account created.                                             |
| 400         | Invalid request body. Account not created.                   |
| 409         | An account with the specified id already exists. Account not created. |

###### Example

Request:

```bash
curl --location --request POST "http://localhost:8888/account/" \
  --header "Content-Type: application/json" \
  --data "{
    \"id\": \"foo\",
    \"balance\": {
        \"currency\": \"EUR\",
        \"value\": \"1234567.89\"
    }
}"
```

Response:

```json
{
  "id": "foo",
  "balance": {
    "value": 1234567.89,
    "currency": "EUR"
  }
}
```

### Update Account

```http
PUT /account/:id/
```

Updates account information, or creates a new account. Request body must include account information in JSON format.

###### Responses

| Status Code | Description                                                  |
| ----------- | ------------------------------------------------------------ |
| 200         | Account created or updated successfully.                     |
| 400         | Invalid request body, or account id in request body does not match account id in the URL. No action was taken. |

###### Example

Request:

```bash
curl --location --request PUT "http://localhost:8888/account/foo/" \
  --header "Content-Type: application/json" \
  --data "{
    \"id\": \"foo\",
    \"balance\": {
        \"currency\": \"USD\",
        \"value\": \"123\"
    }
}"
```

Response:

```json
{
  "id": "foo",
  "balance": {
    "value": 123,
    "currency": "USD"
  }
}
```

### Delete Account

```http
DELETE /account/:id/
```

Deletes an account.

###### Responses

| Status Code | Description                                                  |
| ----------- | ------------------------------------------------------------ |
| 200         | Account deleted. Response includes the account information as of the time of removal. |
| 404         | Account does not exist.                                      |

###### Example

Request:

```bash
curl --location --request DELETE "http://localhost:8888/account/foo/"
```

Response:

```json
{
  "id": "foo",
  "balance": {
    "value": 1234567.89,
    "currency": "EUR"
  }
}
```

### Money Transfer

```http
POST /moneytransfer/
```

Initiates a transfer of funds between two accounts.

The transfer is executed asynchronously, and in order for it to be successful these conditions must be satisfied
at the time of execution:

- Source account, destination account and transfer value must all be denominated in the same currency.
- Source account must have enough available funds.

Note that these conditions might be satisfied at the time of posting the transfer but cease to do so
by the time it is executed.

The API does not provide a facility to track transfer status. Transfer failure can be directly observed by
log traces in the server console, and success can be indirectly observed by absence of said traces and 
retrieving the updated account information. Failed transfers are not reattempted.

###### Responses

| Status Code | Description                                                  |
| ----------- | ------------------------------------------------------------ |
| 202         | Transfer request accepted; the transfer will be scheduled for execution. Note that this is not an indication of any preconditions being satisfied. |
| 400         | Invalid request body.                                        |

###### Example

Request:

```bash
curl --location --request POST "http://localhost:8888/moneytransfer/" \
  --header "Content-Type: application/json" \
  --data "{
    \"from\": \"foo\",
    \"to\": \"bar\",
    \"value\": {
        \"currency\": \"EUR\",
        \"amount\": 50
    }
}"
```

Response:

The response to this request never includes a body.
