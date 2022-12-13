Fleximatcher HTTP interface
============================

## Together with the main project, Fleximatcher, this tool is now archived. Have a look at SpaCy for a recent alternative

A parser and annotator completely accessible through a REST interface, allowing grammar rules to be changed on the fly.

It lets you:

* match substrings and have partial matches on a given text
* add, delete and list the grammar rules and the annotators at runtime
* enrich the text with annotations
* define your own annotators/grammar rules as HTTP services (WordNet, POS taggers, etc.)
* get explanations for a parsing result
* generate a sample text for a given pattern
* match Unicode characters, including of course emojis 😌
* work with space-less languages (e.g. Chinese), as the parser itself is language-agnostic

it's free and ready for use on Docker Hub.

How to use
----------
In general, you use the APIs to define rules and patterns, aggregating them to form more complex ones.

Fleximatcher is completely based on the concept of *annotators*: components which, given a text and an optional parameter string, mark spans of that text with JSON-serialized metadata. The tool comes with built-in annotators for the most common cases (like regular expressions, case insensitive text, unicode characters and annotator combinators), but two of the,m are particularly interesting: the HTTP annotator and the tag annotator. The former calls an HTTP endpoint to produce the needed annotations, making easier to extend the parser; the second implement a rule of a generative grammar, making it possible to produce complex annotations from simpler ones.

In this example, which you can see in `complete_example.sh`,  we'll build an annotator to parse and extract information from recipe procedures, like:
* Boil the sugar, water, lemon and crushed ginger for 15 minutes.
* Strain the mixture and let it cool.
* Mix in the yeast and let the mixture ferment for two days
* Put the drink in bottles and add a raisin to each bottle.

for each step let's try to detect the subject, the action, the tools and additional parameters (time, temperature, etc.).

First, run the application. I suggest to use Docker for greatest comfort:

     docker run -p 4567:4567 jacopofar/fleximatcher-rest-interface

The alternative is to use Maven and follow the `Dockerfile` instructions to build it upon Fleximatcher (the actual parsing library).

Now that we have the service up and running, let's define a new tag, _ingredient_, which will match "water" and create the annotation `{ingredient: "water"}`:

    curl -X POST -H "Content-Type: application/json"  -d '{"pattern":"water", "annotationTemplate":"{ingredient:\"water\"}"}' "http://localhost:4567/tags/ingredient"

the POST will automatically create an identifier for the tag (in the form _tag_X_ where _X_ is the lowest available integer), which will be returned in the response:

    rule for tag ingredient having id ingredient_1 

alternatively, you can use a PUT and specify an id, which will replace any existing tag rule with the given identifier:

    curl -X PUT -H "Content-Type: application/json" -d '{"pattern":"sugar","annotationTemplate":"{ingredient:\"sugar\"}"}' "http://localhost:4567/tags/ingredient/sugar_tag"

the same way we create tags for _sugar_ and _milk_.

A tag is nothing but a production rule of a generative grammar, which is defined based on a string, like in this case, another tag or a mix between them and also annotators (we'll see more about annotators later). Additionally, tags can produce annotations which can be in turn based on the annotations produced by the matched patterns.

So, we can now define a pattern to match an amount of an ingredient, like "_a spoon of sugar_":

    curl -X POST -H "Content-Type: application/json"  -w "\n" -d '{"pattern":"a spoon of of [tag:ingredient]", "annotationTemplate":"{ingredient:#1.ingredient#, amount:\"1\", measure_unit:\"spoons\"}"}' "http://localhost:4567/tags/ingredient_with_amount"

note that the pattern now contains `[tag:ingredient]` which is the tag (aka annotator or production rule). Moreover, the annotation template contains `ingredient:#1.ingredient#`, which is the way we can say "use the ingredient value from the matched element 1 and store it as ingredient". This way, an annotation can use annotations produced by the underlining pattern and pass them upward.

We can test it already:

    curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"text":"There'"'"'s a litre of milk there","pattern":"[tag:ingredient_with_amount]"}' "http://localhost:4567/parse"

this will produce the annotation:

```
{
  "is_matching": true,
  "empty_match": false,
  "time_to_parse": 4,
  "interpretations": [
    {
      "annotations": [
        {
          "annotation": {
            "amount": "1",
            "ingredient": "milk",
            "measure_unit": "liters"
          },
          "span_end": 23,
          "span_start": 8,
          "type": "[tag:ingredient_with_amount]"
        }
      ]
    }
  ]
}
```

the reply means "between position 8 and 23 there's a text which matches the pattern [tag:ingredient_with_amount] and it produced this annotation saying there's 1 liter of water".

Note that interpretations is an array: we can have ambiguity, and the service will in taht case give back all of the interpretations.
The same way we can match a spoon as a measurement unit and whatever comes to our mind. The next logical step is to match an expression like "N liters of water", for any N, we can do that using the rule __r__ (regex):

    curl -X POST -H "Content-Type: application/json"  -w "\n" -d '{"pattern":"[tag:number] litres of [tag:ingredient]", "annotationTemplate":"{ingredient:#2.ingredient#, amount:#0.value#, measure_unit:\"liters\"}"}' "http://localhost:4567/tags/ingredient_with_amount"

other rules are available, like char to match an unicode character, i to match case-insensitively and multi to match a logical AND between expressions. See [here](https://github.com/jacopofar/fleximatcher) for more details. 

and if we match "There's are 34 litres of milk there" we get a surprise: there are 2 interpretations, because the service matches both the 34 and the 4 (ignoring the 3), following the given regex.
We have three possible solutions: one is to pass `"matchWhole": true ` to the parse, which will parse strictly the whole string, otherwise we can change the regex to [r:[^0-9][0-9]*] and add another rule to match [r:^[0-9]+]; the easiest way, however, is to define a *number* tag to match both cases and produce a single annotation format, which also has the advantage of be reusable.


We could do the same for any measurement unit that comes in our mind, but it would be verbose. A smarter thing is to define a tag for all of the ingredients measurement units:

    curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"pattern":"litre","annotationTemplate":"{measure_unit:\"litre\"}"}' "http://localhost:4567/tags/ingredient_measurement_unit"

same goes for spoons, glasses and whatever comes to our mind. Then we can write a more generic rule to match ingredient amounts:

    curl -X POST -H "Content-Type: application/json" -d '{"pattern":"[tag:number] [tag:ingredient_measurement_unit] of [tag:ingredient]", "annotationTemplate":"{ingredient:#4.ingredient#, amount:#0.value#, measure_unit:#2#}"}' "http://localhost:4567/tags/ingredient_with_amount"


Now we have a problem: we defined two patterns for the same thing. We want to remove the older one, so let's list the tags and see the rules attached to them:

    curl http://localhost:4567/tags
    curl http://localhost:4567/tags/ingredient_with_amount

the first call lists the tags, and the second lists the rules for the given one. From there, we see the ids for the old rule, for example _ingredient_with_amount_1_, so with:

    curl -X DELETE -H "Content-Type: application/json"  -d '' "http://localhost:4567/tags/ingredient_with_amount/ingredient_with_amount_3"

this deletes the rule. An important feature of this service is that you can remove rules at runtime, without restarting the parser.

Now we have the capability to recognize ingredients among a list of handwritten ones, but there are hundreds of possible ingredients and write them down is boring to say the least. Why not use an existing database to match all of them ?

English WordNet is a lexical database containing, among other things, hypernym/hyponym relationships between English words, for example it knows that *lemon* is an hyponym of *fruit*. Fleximatcher allows to define an _external HTTP annotator_, that is an HTTP endpoint which expects a POST with a text and a pattern and returns a list of annotations.
In [another repository](https://github.com/jacopofar/wordnet-as-a-service) you can find exactly that kind of service, conveniently available on the Docker Hub as well, so let's see how to use it.

First, let's run the annotator with `docker run --name=worndet_as_a_service -d -p 5679:5679 jacopofar/wordnet-as-a-service`

You can bind an endpoint to a rule with a PUT:
 
    curl -X PUT -H "Content-Type: application/json" -w "\n"  -d '{"endpoint":"http://waas:5679/hyponym/12"}' "http://localhost:4567/rules/en-hypo"

in the POST payload is specified an URL, and on the address a rule name, in this case _en-hypo_ (short for _English Hyponym_). Each time a rule in the form `[en-hypo:something]` or just `[en-hypo]` will be used, a POST request to that endpoint containing the text and the parameter, so that it can give the list of annotations.

In this case, the annotator expects a parameter in the form `w=word` or `w=word,n=class` where word is the word to match hyponyms of and n is the part of the speech (e.g. adjective).
Note: the example scripts uses Docker link function to connect the two services, if you didn't use it you probably want to use your IP for the WNaaS endpoint.

Now the _en-hypo_ rule can be used to match hyponyms of a given english word, so we can define a pattern like:

    curl -X POST -H "Content-Type: application/json"  -w "\n" -d '{"pattern":"[en-hypo:w=vegetable]", "annotationTemplate":"{ingredient:#0#}"}' "http://localhost:4567/tags/ingredient"

and match each type of word marked as vegetable in WordNet as an ingredient.

We can also get samples of the text we could parse, so the application exposes the `/generate` endpoint. The usage is the same of `/parse`, but it doesn't require a text and returns a JSON with the sampled text.

If we use HTTP annotators, we can give an additional sampler endpoint which is used to generate samples for what they annotate. WaaS support it, so with:

    curl -X PUT -H "Content-Type: application/json" -w "\n"  -d '{"endpoint":"http://waas:5679/hyponym/12", "sampler_endpoint":"http://waas:5679/sample/hyponym/12"}' "http://localhost:4567/rules/en-hypo"

we overwrite the en-hypo rule specifying the sampler endpoint.

Using:

      curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"pattern":"[tag:ingredient_with_amount]"}' "http://localhost:4567/generate"

we now obtain sentences like:

* a litre of beetroot
* a spoon of of brussels sprouts
* [r:[^0-9][0-9]*] glass of water

the latest one shows a regex template, because regex annotators currently do not define a sampler function. The same happens if we try to generate a sample with an HTTP annotator without defining a sampler endpoint.




Roadmap
-------

The following functions are ready or to be implemented:

* parse a text and return the corresponding annotations ✓
* add new annotations ✓
* define annotations using HTTP ✓
* generate text from patterns ✓
* explicitly explain all the steps leading to a parse result ✓
* give said steps in a human-readable report (not a JSON) ☐
* export and import all the rules in a single file ✓
* return statistics on the average parse duration and the usage of the different tags ☐
* define parsers using Javascript ☐
