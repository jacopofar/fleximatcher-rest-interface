package com.github.jacopofar.fleximatcherwebinterface;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jacopofar.fleximatcher.FlexiMatcher;
import com.github.jacopofar.fleximatcher.annotations.MatchingResults;
import com.github.jacopofar.fleximatcher.annotations.TextAnnotation;
import com.github.jacopofar.fleximatcher.importer.FileTagLoader;
import com.github.jacopofar.fleximatcherwebinterface.annotators.HTTPRuleFactory;
import com.github.jacopofar.fleximatcherwebinterface.exceptions.RuntimeJSONCarryingException;
import com.github.jacopofar.fleximatcherwebinterface.messages.AnnotatorPayload;
import com.github.jacopofar.fleximatcherwebinterface.messages.CompleteTagPayload;
import com.github.jacopofar.fleximatcherwebinterface.messages.ParseRequestPayload;
import com.github.jacopofar.fleximatcherwebinterface.messages.TagRulePayload;
import org.json.JSONException;
import org.json.JSONObject;
import spark.Response;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static spark.Spark.*;

public class Fwi {
    private static FlexiMatcher fm;
    private static  ConcurrentHashMap<String,AnnotatorPayload> annotators=new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Starting matcher...");
        fm = new FlexiMatcher();
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");

        String fname="rule_list.tsv";
        FileTagLoader.readTagsFromTSV(fname, fm);



        //staticFiles.externalLocation("static");

        //staticFiles.externalLocation("/static");
        //  port(5678); <- Uncomment this if you want spark to listen to port 5678 in stead of the default 4567

        //show exceptions in console and HTTP responses
        exception(Exception.class, (exception, request, response) -> {
            //show the exceptions using stdout
            System.out.println("Exception dealing with " + request.requestMethod() + " " + request.contextPath());
            exception.printStackTrace(System.out);
            response.status(400);
            if(exception instanceof RuntimeJSONCarryingException){
                response.type("application/json; charset=utf-8");
                response.body(exception.toString());
                return;
            }
            response.type("application/json; charset=utf-8");
            response.body(exception.toString());

        });

        //show each requested path in the console
        after((request, response) -> {
            //TODO currently not working, waiting for this merge to appear in Maven:
            //https://github.com/perwendel/spark/pull/579
            /*
            System.out.println(ZonedDateTime.now().format( DateTimeFormatter.ISO_INSTANT)
                    + " \t " + request.requestMethod()
                    + " \t " + request.pathInfo()
                    + " \t " + response.status()
                    + " \t " + response.body().length());*/
        });


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


        put("/tags/:tagname/:tag_identifier", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            TagRulePayload newPost = mapper.readValue(request.body(), TagRulePayload.class);
            if(newPost.errorMessages().size() != 0){
                response.status(400);
                return "invalid request body. Errors: " + newPost.errorMessages() ;
            }
            System.out.println("RULE TO BE CREATED: " + newPost.toString());
            fm.addTagRule(request.params(":tagname"),newPost.getPattern(), request.params(":tag_identifier"),newPost.getAnnotationTemplate());
            return "rule for tag " + request.params(":tagname") + " having id " + request.params(":tag_identifier") + " : " + newPost.toString();
        });

        post("/tags/:tagname", (request, response) -> {
            String tagName = request.params(":tagname");
            int newId = 1;

            while(fm.getTagRule(tagName, tagName + "_" + newId).isPresent()){
                newId++;
            }
            ObjectMapper mapper = new ObjectMapper();
            TagRulePayload newPost = mapper.readValue(request.body(), TagRulePayload.class);
            if(newPost.errorMessages().size() != 0){
                response.status(400);
                return "invalid request body. Errors: " + newPost.errorMessages() ;
            }
            System.out.println("RULE TO BE CREATED: " + newPost.toString());
            fm.addTagRule(tagName,newPost.getPattern(), tagName + "_" + newId,newPost.getAnnotationTemplate());

            return "rule for tag " + tagName + " having id " + tagName + "_" + newId + " : " + newPost.toString();
        });

        post("/parse", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            ParseRequestPayload parseRequest = mapper.readValue(request.body(), ParseRequestPayload.class);
            if(parseRequest.errorMessages().size() != 0){
                response.status(400);
                return "invalid request body. Errors: " + parseRequest.errorMessages() ;
            }
            System.out.println("PARSE REQUEST: " + parseRequest.toString());
            MatchingResults results;
            JSONObject retVal = new JSONObject();
            long start=System.currentTimeMillis();
            //the flags are: fullyAnnotate,  matchWhole, populateResult

            results = fm.matches(parseRequest.getText(),parseRequest.getPattern(),FlexiMatcher.getDefaultAnnotator(), parseRequest.isFullyAnnotate(), parseRequest.isMatchWhole(), parseRequest.isPopulateResult());

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

        /**
         * Add a new annotator
         * */
        put("/rules/:rulename", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            AnnotatorPayload newPost = mapper.readValue(request.body(), AnnotatorPayload.class);
            if(newPost.errorMessages().size() != 0){
                response.status(400);
                return "invalid request body. Errors: " + newPost.errorMessages();
            }
            System.out.println("Creating annotator for: " + newPost.toString());
            boolean replaced = fm.bind(request.params(":rulename"), new HTTPRuleFactory(newPost.getEndpoint(), newPost.getSampler_endpoint()));
            annotators.put(request.params(":rulename"), newPost);
            return (replaced ? "annotator updated": "annotator added") + " for rule " + request.params(":rulename");
        });

        /**
         * Retrieve annotator details
         * */
        get("/rules/:rulename", (request, response) -> {
            response.type("application/json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(annotators.get(request.params(":rulename")));
        });

        /**
         * Delete a specific tag rule
         * */
        delete("/tags/:tagname/:tag_identifier", (request, response) -> {
            if(!fm.getTagNames().anyMatch(n -> n.equals(request.params(":tagname")))){
                response.status(404);
                return "tag " + request.params(":tagname") + " unknown";
            }
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

        /**
         * List the known rules for a given tag
         * */
        get("/tags/:tagname", (request, response) -> {
            response.type("application/json");
            ServletOutputStream os = response.raw().getOutputStream();
            os.write("[".getBytes());
            final boolean[] first = {true};
            fm.getTagDefinitions(request.params(":tagname")).forEach( td -> {
                try {
                    if(!first[0]) os.write(",".getBytes());
                    first[0] = false;
                    JSONObject jo = new JSONObject();
                    jo.put("id", td.getIdentifier());
                    jo.put("pattern", td.getPattern());
                    if(td.getAnnotationExpression() != null)
                        jo.put("annotation", td.getAnnotationExpression());
                    os.write(jo.toString().getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    //can never happen, key is hardcoded and not null
                    e.printStackTrace();
                }
            });
            os.write("]".getBytes());

            os.flush();
            os.close();
            return response.raw();
        });

        /**
         * Explains with a human-readable text how an interpretation was given
         * */
        post("/parse_explain", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            ParseRequestPayload parseRequest = mapper.readValue(request.body(), ParseRequestPayload.class);
            if(parseRequest.errorMessages().size() != 0){
                response.status(400);
                return "invalid request body. Errors: " + parseRequest.errorMessages() ;
            }
            System.out.println("PARSE REQUEST: " + parseRequest.toString());
            MatchingResults results;
            JSONObject retVal = new JSONObject();
            long start=System.currentTimeMillis();
            //the flags are: fullyAnnotate,  matchWhole, populateResult

            results = fm.matches(parseRequest.getText(),parseRequest.getPattern(),FlexiMatcher.getDefaultAnnotator(true), true, parseRequest.isMatchWhole(), true);

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



        post("/generate", (request, response) -> {
            String pattern = new JSONObject(request.body()).getString("pattern");

            System.out.println("REQUEST TO GENERATE SAMPLE FOR: " + pattern);
            JSONObject retVal = new JSONObject();
            long start=System.currentTimeMillis();
            String sample = fm.generateSample(pattern);
            retVal.put("time_to_generate", System.currentTimeMillis()-start);
            retVal.put("sample", sample);
            return sendJSON(response, retVal);
        });

        get("/export_full", (request, response) -> {
            response.type("application/json");

            ServletOutputStream os = response.raw().getOutputStream();
            os.write("[".getBytes());
            final boolean[] first = {true};
            fm.getTagNames().forEach(tagName -> {
                fm.getTagDefinitions(tagName).forEach( td -> {
                    try {
                        if(!first[0]) os.write(",".getBytes());
                        first[0] = false;
                        JSONObject jo = new JSONObject();
                        jo.put("tag_name", tagName);
                        jo.put("id", td.getIdentifier());
                        jo.put("pattern", td.getPattern());
                        if(td.getAnnotationExpression() != null)
                            jo.put("annotation", td.getAnnotationExpression());
                        os.write(jo.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        //can never happen, key is hardcoded and not null
                        e.printStackTrace();
                    }
                });
            });

            os.write("]".getBytes());

            os.flush();
            os.close();
            return response.raw();
        });

        post("/import_full", (request, response) -> {
            ObjectMapper mapper = new ObjectMapper();
            List<CompleteTagPayload> tagsToImport = mapper.readValue(request.body(), mapper.getTypeFactory().constructCollectionType(List.class, CompleteTagPayload.class));
            LinkedList<String> overWritten = new LinkedList<String>();
            for(CompleteTagPayload newTag: tagsToImport){
                if(fm.addTagRule(newTag.getTag_name(), newTag.getPattern(), newTag.getId(), newTag.getAnnotation()))
                    overWritten.add(newTag.getTag_name() + "/" + newTag.getId());
            }
            JSONObject retVal = new JSONObject();
            retVal.put("imported", tagsToImport.size());
            retVal.put("overwritten", overWritten);
            return sendJSON(response, retVal);

        });
    }

    private static String sendJSON(Response r, JSONObject obj) {
        r.type("application/json");
        return obj.toString();
    }
}