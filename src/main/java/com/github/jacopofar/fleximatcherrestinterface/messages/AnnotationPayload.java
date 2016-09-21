package com.github.jacopofar.fleximatcherrestinterface.messages;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created on 2016-06-29.
 */
public class AnnotationPayload {
        Integer start;
        Integer end;

        public ObjectNode getAnnotation() {
            return annotation;
        }

        public void setAnnotation(ObjectNode annotation) {
            this.annotation = annotation;
        }

        public Integer getStart() {
            return start;
        }

        public void setStart(Integer start) {
            this.start = start;
        }

        public Integer getEnd() {
            return end;
        }

        public void setEnd(Integer end) {
            this.end = end;
        }

        ObjectNode annotation;

}
