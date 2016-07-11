package com.github.jacopofar.fleximatcherrestbinterface.messages;

/**
 * Created on 2016-06-17.
 */
public class CompleteTagPayload {
    String annotation;

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getTag_name() {
        return tag_name;
    }

    public void setTag_name(String tag_name) {
        this.tag_name = tag_name;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    String tag_name;
    String pattern;
    String id;
}
