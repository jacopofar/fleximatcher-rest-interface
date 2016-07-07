package com.github.jacopofar.fleximatcherwebinterface.annotators;

import com.github.jacopofar.fleximatcher.rule.RuleFactory;
import com.github.jacopofar.fleximatcher.rules.MatchingRule;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created on 2016-06-29.
 */
public class HTTPRuleFactory implements RuleFactory {

    private final URL url;
    private URL samplerUrl;

    public HTTPRuleFactory(String annotatorUrl, String samplerUrl) throws MalformedURLException {
        this.url = new URL(annotatorUrl);
        if(samplerUrl != null)
            this.samplerUrl = new URL(samplerUrl);
    }

    @Override
    public MatchingRule getRule(String parameter) {
        return new HTTPRule(url, parameter);
    }

    @Override
    public String generateSample(String parameter) {
        if (this.samplerUrl == null)
            return null;
        try {
            String result = Unirest.post(samplerUrl.toString())
                    .header("content-type", "application/json")
                    .body("{\"parameter\":" + JSONObject.quote(parameter) +  "}")
                    .asString().getBody();
            //The empty string is the way an HTTP annotator tells it failed to generate an utterance. Use null
            return result.isEmpty() ? null : result;
        } catch (UnirestException e) {
            e.printStackTrace();
            throw new RuntimeException("error generating sample from external annotator",e);
        }
    }
}
