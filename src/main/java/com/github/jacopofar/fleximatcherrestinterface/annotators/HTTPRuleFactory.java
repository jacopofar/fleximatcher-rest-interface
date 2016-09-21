package com.github.jacopofar.fleximatcherrestinterface.annotators;

import com.github.jacopofar.fleximatcher.rule.RuleFactory;
import com.github.jacopofar.fleximatcher.rules.MatchingRule;
import com.github.jacopofar.fleximatcherrestinterface.exceptions.RuntimeJSONCarryingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.conn.HttpHostConnectException;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created on 2016-06-29.
 */
public class HTTPRuleFactory implements RuleFactory {

    private final URL url;
    private final URL samplerUrl;
    private final String apiKey;

    public HTTPRuleFactory(String annotatorUrl, String samplerUrl, String apiKey) throws MalformedURLException {
        this.url = new URL(annotatorUrl);
        this.samplerUrl = samplerUrl == null ? null : new URL(samplerUrl);
        this.apiKey = apiKey;
    }

    @Override
    public MatchingRule getRule(String parameter) {
        return new HTTPRule(url, parameter, apiKey);
    }

    @Override
    public String generateSample(String parameter) {
        if (this.samplerUrl == null)
            return null;
        try {
            HttpResponse<String> response;
            if(apiKey != null){
                response = Unirest.post(samplerUrl.toString())
                        .header("content-type", "application/json")
                        .header("X-API-KEY", apiKey)
                        .body("{\"parameter\":" + JSONObject.quote(parameter) +  ", \"api_key\":" + JSONObject.quote(apiKey) +  "}")
                        .asString();

            }
            else{
                response = Unirest.post(samplerUrl.toString())
                        .header("content-type", "application/json")
                        .body("{\"parameter\":" + JSONObject.quote(parameter) +  "}")
                        .asString();
            }
            if(response.getStatus() != 200){
                JSONObject errObj = new JSONObject();
                try {
                    errObj.put("http_status",response.getStatus());
                    errObj.put("endpoint",samplerUrl.toString());
                    errObj.put("error_body",response.getBody());
                } catch (JSONException e) {
                    //can never happen...
                    e.printStackTrace();
                }
                throw new RuntimeJSONCarryingException("Error from the external annotator", errObj);
            }
            //The empty string is the way an HTTP annotator tells it failed to generate an utterance. Use null
            return response.getBody().isEmpty() ? null : response.getBody();
        } catch (UnirestException e) {
            if(e.getCause() instanceof HttpHostConnectException){
                e.printStackTrace();
                JSONObject errObj = new JSONObject();
                try {
                    errObj.put("error_message",e.getCause().getMessage());
                    errObj.put("endpoint",samplerUrl.toString());
                } catch (JSONException jex) {
                    //can never happen...
                    jex.printStackTrace();
                }
                throw new RuntimeJSONCarryingException("Error communicating with the external annotator", errObj);
            }
            e.printStackTrace();
            throw new RuntimeException("error generating sample from external annotator",e);
        }
    }
}
