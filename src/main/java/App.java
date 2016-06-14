import com.github.jacopofar.fleximatcher.FlexiMatcher;
import com.github.jacopofar.fleximatcher.annotations.MatchingResults;
import com.github.jacopofar.fleximatcher.annotations.TextAnnotation;
import com.github.jacopofar.fleximatcher.importer.FileTagLoader;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import static spark.Spark.*;

/**
 * A simple example just showing some basic functionality
 */
public class App {
    private static FlexiMatcher fm;
    private static FileWriter fwTag;
    private static int tagCount=0;

    public static void main(String[] args) throws IOException {

        System.out.println("starting matcher...");
        fm = new FlexiMatcher();

        /*
        fm.bind("it-pos", new ItPosRuleFactory(im));
        fm.bind("it-token", new ItTokenRuleFactory(im));
        fm.bind("it-verb-conjugated", new ItSpecificVerbRuleFactory(im));
        fm.bind("it-verb-form", new ItVerbFormRuleFactory(im));
        */
        String fname="rule_list.tsv";
        FileTagLoader.readTagsFromTSV(fname, fm);

        fwTag=new FileWriter(fname,true);


        staticFiles.location("/");
        //staticFiles.externalLocation("/static");
        //  port(5678); <- Uncomment this if you want spark to listen to port 5678 in stead of the default 4567

        get("/parse", (request, response) -> {

            JSONObject ret = new JSONObject();
            String text = request.queryMap().get("text").value();
            String pattern = request.queryMap().get("pattern").value();
            System.out.println(text+" -- "+pattern);
            MatchingResults results;
            try{
                long start=System.currentTimeMillis();
                results = fm.matches(text, pattern, FlexiMatcher.getDefaultAnnotator(), true, false, true);
                ret.put("time to parse", System.currentTimeMillis()-start);
            }
            catch(RuntimeException r){
                r.printStackTrace();
                return (("{\"error\":"+JSONObject.quote(r.getMessage())+"}").getBytes("UTF-8"));
            }
            System.out.println(results.toString());
            JSONObject retVal = new JSONObject();
            retVal.put("is_matching", results.isMatching());
            retVal.put("empty_match", results.isEmptyMatch());
            for(LinkedList<TextAnnotation> interpretation:results.getAnnotations().get()){
                JSONObject addMe = new JSONObject();

                for(TextAnnotation v:interpretation){
                    addMe.append("annotations", new JSONObject(v.toJSON()));
                }
                ret.append("interpretations", addMe);
            }
            return ret.toString();
        });



    }
}