package com.github.jacopofar.fleximatcherwebinterface;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jacopofar.fleximatcher.FlexiMatcher;
import com.github.jacopofar.fleximatcher.annotations.MatchingResults;
import com.github.jacopofar.fleximatcher.annotations.TextAnnotation;
import com.github.jacopofar.fleximatcher.importer.FileTagLoader;
import com.github.jacopofar.fleximatcherwebinterface.messages.TagRulePayload;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;

import static spark.Spark.*;

/**
 * A simple example just showing some basic functionality
 */
public class Fwi {
    private static FlexiMatcher fm;
    private static int tagCount=0;

    public static void main(String[] args) throws IOException {

        System.out.println("starting matcher...");
        fm = new FlexiMatcher();
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        /*
        fm.bind("it-pos", new ItPosRuleFactory(im));
        fm.bind("it-token", new ItTokenRuleFactory(im));
        fm.bind("it-verb-conjugated", new ItSpecificVerbRuleFactory(im));
        fm.bind("it-verb-form", new ItVerbFormRuleFactory(im));
        */
        String fname="rule_list.tsv";
        FileTagLoader.readTagsFromTSV(fname, fm);



        staticFiles.externalLocation("static");

        //staticFiles.externalLocation("/static");
        //  port(5678); <- Uncomment this if you want spark to listen to port 5678 in stead of the default 4567

        get("/parse", (request, response) -> {

            JSONObject retVal = new JSONObject();
            String text = request.queryMap().get("text").value();
            String pattern = request.queryMap().get("pattern").value();
            System.out.println(text+" -- "+pattern);
            MatchingResults results;
            try{
                long start=System.currentTimeMillis();
                results = fm.matches(text, pattern, FlexiMatcher.getDefaultAnnotator(), true, false, true);
                retVal.put("time_to_parse", System.currentTimeMillis()-start);
            }
            catch(RuntimeException r){
                System.out.println(r.getMessage());
                r.printStackTrace();
                return "{\"error\":"+JSONObject.quote(r.getMessage())+"}";
            }
            System.out.println(results.toString());
            retVal.put("is_matching", results.isMatching());
            retVal.put("empty_match", results.isEmptyMatch());
            for(LinkedList<TextAnnotation> interpretation:results.getAnnotations().get()){
                JSONObject addMe = new JSONObject();

                for(TextAnnotation v:interpretation){
                    addMe.append("annotations", new JSONObject(v.toJSON()));
                }
                retVal.append("interpretations", addMe);
            }

            return retVal.toString();
        });


        put("/tagrule", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();

            TagRulePayload newPost = mapper.readValue(request.body(), TagRulePayload.class);
            if(newPost.errorMessages().size() != 0){
                response.status(400);
                return "invalid request body. Errors: " + newPost.errorMessages() ;
            }
            System.out.println("RULE TO BE CREATED: " + newPost.toString());
            fm.addTagRule(newPost.getTag(),newPost.getPattern(), newPost.getIdentifier(),newPost.getAnnotationTemplate());
            return "rule created: " + newPost.toString();
        });

        exception(Exception.class, (exception, request, response) -> {
            //show the exceptions using stdout
            System.out.println("Exception!");
            exception.printStackTrace(System.out);

            response.body(exception.getMessage());
        });


    }
}