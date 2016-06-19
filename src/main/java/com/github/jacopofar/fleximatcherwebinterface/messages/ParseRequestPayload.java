package com.github.jacopofar.fleximatcherwebinterface.messages;

import java.util.LinkedList;

/**
 * Created by utente on 2016-06-19.
 */
public class ParseRequestPayload {
    String text;
    String pattern;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public LinkedList<String> errorMessages() {
        LinkedList<String> ret = new LinkedList<String>();
        if(pattern == null)
            ret.push("no pattern specified");
        if(text == null)
            ret.push("no text specified");
        return ret;
    }
}
