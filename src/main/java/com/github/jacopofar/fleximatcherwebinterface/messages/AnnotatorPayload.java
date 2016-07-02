package com.github.jacopofar.fleximatcherwebinterface.messages;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Describes a new HTTP annotator
 */
public class AnnotatorPayload {
    String endpoint;
    String api_key;
    String sampler_endpoint;

    public String getSampler_endpoint() {
        return sampler_endpoint;
    }

    public void setSampler_endpoint(String sampler_endpoint) {
        this.sampler_endpoint = sampler_endpoint;
    }

    public String getApi_key() {
        return api_key;
    }

    public void setApi_key(String api_key) {
        this.api_key = api_key;
    }


    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public List<String> errorMessages() {
        LinkedList<String> ret = new LinkedList<String>();
        if(endpoint == null)
            ret.push("no endpoint specified");
        try {
            new URL(this.endpoint);
        } catch (MalformedURLException e) {
            ret.push("invalid URL");
        }
        return ret;
    }
}
