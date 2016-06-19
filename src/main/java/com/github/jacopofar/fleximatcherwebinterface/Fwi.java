package com.github.jacopofar.fleximatcherwebinterface;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jacopofar.fleximatcher.FlexiMatcher;
import com.github.jacopofar.fleximatcher.annotations.MatchingResults;
import com.github.jacopofar.fleximatcher.annotations.TextAnnotation;
import com.github.jacopofar.fleximatcher.importer.FileTagLoader;
import com.github.jacopofar.fleximatcherwebinterface.messages.ParseRequestPayload;
import com.github.jacopofar.fleximatcherwebinterface.messages.TagRulePayload;
import org.json.JSONObject;
import spark.Response;

import javax.servlet.ServletOutputStream;
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

            String text = request.queryMap().get("text").value();
            String pattern = request.queryMap().get("pattern").value();
            System.out.println(text+" -- "+pattern);

            MatchingResults results;
            JSONObject retVal = new JSONObject();
            long start=System.currentTimeMillis();
            results = fm.matches(text, pattern, FlexiMatcher.getDefaultAnnotator(), true, false, true);
            retVal.put("time_to_parse", System.currentTimeMillis()-start);
            retVal.put("is_matching", results.isMatching());
            retVal.put("empty_match", results.isEmptyMatch());
            for(LinkedList<TextAnnotation> interpretation:results.getAnnotations().get()){
                JSONObject addMe = new JSONObject();

                for(TextAnnotation v:interpretation){
                    addMe.append("annotations", new JSONObject(v.toJSON()));
                }
                retVal.append("interpretations", addMe);
            }
            return sendJSON(response, retVal);

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

        post("/parse", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            ParseRequestPayload newPost = mapper.readValue(request.body(), ParseRequestPayload.class);
            if(newPost.errorMessages().size() != 0){
                response.status(400);
                return "invalid request body. Errors: " + newPost.errorMessages() ;
            }


            MatchingResults results;
            JSONObject retVal = new JSONObject();
            long start=System.currentTimeMillis();
            //TODO allow the request to specify parsing flags
            results = fm.matches(newPost.getText(),newPost.getPattern(),FlexiMatcher.getDefaultAnnotator(), true, false, true);

            retVal.put("time_to_parse", System.currentTimeMillis()-start);
            retVal.put("is_matching", results.isMatching());
            retVal.put("empty_match", results.isEmptyMatch());
            if(results.isMatching()){
                for(LinkedList<TextAnnotation> interpretation:results.getAnnotations().get()){
                    JSONObject addMe = new JSONObject();

                    for(TextAnnotation v:interpretation){
                        addMe.append("annotations", new JSONObject(v.toJSON()));
                    }
                    retVal.append("interpretations", addMe);
                }
            }
            return sendJSON(response, retVal);
        });

        exception(Exception.class, (exception, request, response) -> {
            //show the exceptions using stdout
            System.out.println("Exception:");
            exception.printStackTrace(System.out);

            response.body(exception.getMessage());
        });

        /**
         * Delete a specific tag rule
         * */
        delete("/tagrule/:tagname/:tag_identifier", (request, response) -> {
            if(fm.removeTagRule(request.params(":tagname"), request.params(":tag_identifier"))){
                response.status(200);
                return "rule removed";
            }
            else {
                response.status(404);
                return "rule not found";
            }
        });

        /**
         * List the known tags
         * */
        get("/tags", (request, response) -> {
            response.type("application/json");
            ServletOutputStream os = response.raw().getOutputStream();
            os.write("[".getBytes());
            final boolean[] first = {true};
            fm.getTagNames().forEach( tn -> {
                try {
                    if(!first[0]) os.write(",".getBytes());
                    first[0] = false;
                    os.write(JSONObject.quote(tn).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            os.write("]".getBytes());

            os.flush();
            os.close();
            return response.raw();

        });

    }

    private static String sendJSON(Response r, JSONObject obj) {
        r.type("application/json");
        return obj.toString();
    }
}