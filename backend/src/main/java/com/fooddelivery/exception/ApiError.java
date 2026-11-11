package com.fooddelivery.exception;

import java.util.Map;

public class ApiError {

    private String message;
    private Map<String, String> errors;

    public ApiError(String message) {
        this.message = message;
    }

    public ApiError(String message, Map<String, String> errors) {
        this.message = message;
        this.errors = errors;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
