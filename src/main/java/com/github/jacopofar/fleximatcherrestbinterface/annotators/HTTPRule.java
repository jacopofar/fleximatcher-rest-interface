package com.github.jacopofar.fleximatcherrestbinterface.annotators;

import com.github.jacopofar.fleximatcher.annotations.AnnotationHandler;
import com.github.jacopofar.fleximatcher.rules.MatchingRule;
import com.github.jacopofar.fleximatcherrestbinterface.exceptions.RuntimeJSONCarryingException;
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
 * MatchingRule which just sends the text and the parameter to a previously defined endpoint and parses the resulting JSON
 */
public class HTTPRule extends MatchingRule {

    private final URL endpoint;
    private final String parameter;
    private final String apiKey;

    public HTTPRule(URL endpoint, String parameter, String apiKey) {
        super();
        this.endpoint = endpoint;
        this.parameter = parameter;
        this.apiKey = apiKey;
    }

    @Override
    public boolean annotate(String text, AnnotationHandler ah) {
        boolean totalMatch = false;
        try {
            HttpResponse<JsonNode> response;
            if(apiKey == null){
                response = Unirest.post(endpoint.toString())
                        .header("content-type", "application/json")
                        .header("X-API-KEY", apiKey)
                        .body("{\"parameter\":" + JSONObject.quote(parameter) +  ",\"text\":" + JSONObject.quote(text) + ", \"api_key\":\" + JSONObject.quote(apiKey) +  \"}")
                        .asJson();

            }
            else{
                response = Unirest.post(endpoint.toString())
                        .header("content-type", "application/json")
                        .body("{\"parameter\":" + JSONObject.quote(parameter) +  ",\"text\":" + JSONObject.quote(text) + "}")
                        .asJson();

            }
            if(response.getBody().getObject().has("error")){
                System.err.println("the external annotator gave an HTTP error: " + response.getBody().getObject().get("error").toString());
                throw new RuntimeJSONCarryingException("Error from the external annotator", response.getBody().getObject());
            }
            JSONArray annotations = response.getBody().getObject().getJSONArray("annotations");

            for(int i = 0 ; i < annotations.length(); i++){
                JSONObject ap = annotations.getJSONObject(i);
                if (ap.getInt("span_start") == 0 && ap.getInt("span_end") == text.length()){
                    totalMatch = true;
                }
                ah.addAnnotation(new Span(ap.getInt("span_start"), ap.getInt("span_end")), ap.has("annotation") ? ap.getJSONObject("annotation") : null);
            }
        } catch (UnirestException e) {
            e.printStackTrace();
            if (e.getCause() instanceof org.json.JSONException) {
                throw new RuntimeException("error parsing the data from the annotator, not a valid JSON: " + e.getMessage(),e);
            }
            else{
                throw new RuntimeException("error communicating with external annotator " + e.getMessage(),e);
            }
        }  catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("error parsing external annotator response",e);
        }

        return totalMatch;
    }
}
