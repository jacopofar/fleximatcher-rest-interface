package com.github.jacopofar.fleximatcherwebinterface.messages;

import java.util.LinkedList;

/**
 * Created on 2016-06-28.
 */
public class AnnotatorPayload {
    String URL;

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public LinkedList<String> errorMessages() {
        //TODO implement this
    }
}
