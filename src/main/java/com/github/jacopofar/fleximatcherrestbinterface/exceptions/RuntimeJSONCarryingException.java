package com.github.jacopofar.fleximatcherrestbinterface.exceptions;

import org.json.JSONObject;

/**
 * Created by utente on 2016-07-07.
 */
public class RuntimeJSONCarryingException extends RuntimeException {
    String message;
    JSONObject details;


    @Override
    public String getMessage() {
        return message;
    }

    public JSONObject getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "{\"message\":" + JSONObject.quote(message)
                +", \"details\":" + details.toString() + "}";
    }

    public RuntimeJSONCarryingException(String message, JSONObject details) {
        this.message = message;
        this.details = details;
    }
}
