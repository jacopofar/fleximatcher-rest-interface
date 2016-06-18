package com.github.jacopofar.fleximatcherwebinterface.messages;

import java.util.LinkedList;

/**
 * Created on 2016-06-17.
 */
public class TagRulePayload {
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
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



    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    private String pattern;
    private String identifier;
    private String annotationTemplate;
    private String tag;

    @Override
    public String toString() {
        return "TagRulePayload{" +
                "pattern='" + pattern + '\'' +
                ", identifier='" + identifier + '\'' +
                ", annotationTemplate='" + annotationTemplate + '\'' +
                ", tag='" + tag + '\'' +
                '}';
    }

    public LinkedList<String> errorMessages() {
        LinkedList<String> ret = new LinkedList<String>();
        if(pattern == null)
            ret.push("no pattern specified");
        if(identifier == null)
            ret.push("no identifier specified");
        if(annotationTemplate == null)
            ret.push("no annotationTemplate specified");
        return ret;
    }
}
