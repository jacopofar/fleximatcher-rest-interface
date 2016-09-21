package com.github.jacopofar.fleximatcherrestinterface.messages;

import java.util.LinkedList;

/**
 * Created on 2016-06-19.
 */
public class ParseRequestPayload {
    String text;
    String pattern;
    boolean fullyAnnotate = true;
    boolean matchWhole = false;
    boolean populateResult = true;

    public boolean isFullyAnnotate() {
        return fullyAnnotate;
    }

    public void setFullyAnnotate(boolean fullyAnnotate) {
        this.fullyAnnotate = fullyAnnotate;
    }

    @Override
    public String toString() {
        return "ParseRequestPayload{" +
                "text='" + text + '\'' +
                ", pattern='" + pattern + '\'' +
                ", fullyAnnotate=" + fullyAnnotate +
                ", matchWhole=" + matchWhole +
                ", populateResult=" + populateResult +
                '}';
    }

    public boolean isMatchWhole() {
        return matchWhole;
    }

    public void setMatchWhole(boolean matchWhole) {
        this.matchWhole = matchWhole;
    }

    public boolean isPopulateResult() {
        return populateResult;
    }

    public void setPopulateResult(boolean populateResult) {
        this.populateResult = populateResult;
    }



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
