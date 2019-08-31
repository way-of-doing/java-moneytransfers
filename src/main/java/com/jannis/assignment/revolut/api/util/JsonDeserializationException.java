package com.jannis.assignment.revolut.api.util;

class JsonDeserializationException extends RuntimeException {
    JsonDeserializationException() {
    }

    JsonDeserializationException(String message) {
        super(message);
    }
}
