package com.github.jacopofar.fleximatcherwebinterface.annotators;

import com.github.jacopofar.fleximatcher.annotations.AnnotationHandler;
import com.github.jacopofar.fleximatcher.rules.MatchingRule;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import opennlp.tools.util.Span;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

/**
 * Created on 2016-06-29.
 */
public class HTTPRule extends MatchingRule {

    private final URL endpoint;
    private final String parameter;

    public HTTPRule(URL endpoint, String parameter) {
        super();
        this.endpoint = endpoint;
        this.parameter = parameter;
    }

    @Override
    public boolean annotate(String text, AnnotationHandler ah) {
        boolean totalMatch = false;
        try {
            HttpResponse<JsonNode> response = Unirest.post(endpoint.toString())
                    .header("content-type", "application/json")
                    .body("{\"parameter\":" + JSONObject.quote(parameter) +  ",\"text\":" + JSONObject.quote(text) + "}")
                    .asJson();
            JSONArray annotations = response.getBody().getObject().getJSONArray("annotations");

            for(int i = 0 ; i < annotations.length(); i++){
                JSONObject ap = annotations.getJSONObject(i);
                if (ap.getInt("span_start") == 0 && ap.getInt("span_end") == text.length()){
                    totalMatch = true;
                }
                ah.addAnnotation(new Span(ap.getInt("span_start"), ap.getInt("span_end")), ap.has("annotation") ? null : ap.getJSONObject("annotation"));
            }
        } catch (UnirestException e) {
            e.printStackTrace();
            throw new RuntimeException("error communicating with external annotator",e);
        }  catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("error parsing external annotator response",e);
        }

        return totalMatch;
    }
}
