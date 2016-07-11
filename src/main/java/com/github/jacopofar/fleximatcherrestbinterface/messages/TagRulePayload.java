package com.github.jacopofar.fleximatcherrestbinterface.messages;

import java.util.LinkedList;

/**
 * Created on 2016-06-17.
 */
public class TagRulePayload {
    @Override
    public String toString() {
        return "TagRulePayload{" +
                "pattern='" + pattern + '\'' +
                ", annotationTemplate='" + annotationTemplate + '\'' +
                '}';
    }

    public String getAnnotationTemplate() {
        return annotationTemplate;
    }

    public void setAnnotationTemplate(String annotationTemplate) {
        this.annotationTemplate = annotationTemplate;
    }

    public String getPattern() {

        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }


    private String pattern;
    private String annotationTemplate;


    public LinkedList<String> errorMessages() {
        LinkedList<String> ret = new LinkedList<String>();
        if(pattern == null)
            ret.push("no pattern specified");
        return ret;
    }
}
