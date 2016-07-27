Fleximatecher HTTP interface
============================

A parser/annotator completely accessible through a REST interface, allowing grammar rules to be changed on the fly.

It lets you:

* match substrings and have partial matches
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

In this example, which you can see in `complete_example.sh`,  we'll build an annotator to parse and extract information from recipe procedures, like:
* Boil the sugar, water, lemon and crushed ginger for 15 minutes.
* Strain the mixture and let it cool.
* Mix in the yeast and let the mixture ferment for two days
* Put the drink in bottles and add a raisin to each bottle.

for each step let's detect the subject, the action, the tools and additional parameters (time, temperature, etc.).

First, run the application. I warmly suggest to use Docker:

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

    curl -X POST -H "Content-Type: application/json"  -w "\n" -d '{"pattern":"[r:[0-9]+] litres of [tag:ingredient]", "annotationTemplate":"{ingredient:#2.ingredient#, amount:#0#, measure_unit:\"liters\"}"}' "http://localhost:4567/tags/ingredient_with_amount"

other rules are available, like char to match an unicode character, i to match case-insensitively and multi to match a logical AND between expressions. See [here](https://github.com/jacopofar/fleximatcher) for more details. 

and if we match "There's are 34 litres of milk there" we get a surprise: there are 2 interpretations, because the service matches both the 34 and the 4 (ignoring the 3), following the given regex.
We have two solutions: one is to pass `"matchWhole": true ` to the parse, which will parse strictly the whole string, or change the regex to [r:[^0-9][0-9]+] and add another rule to match [r:^[0-9]+]

We could do the same for any measurement unit that comes in our mind, but it would be verbose. A smarter thing is to define a tag for all of the ingredients measurement units:

    curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"pattern":"litre","annotationTemplate":"{measure_unit:\"litre\"}"}' "http://localhost:4567/tags/ingredient_measurement_unit"

same goes for spoons, glasses and whatever comes to our mind. Then we can write a more generic rule to match ingredient amounts:

    curl -X POST -H "Content-Type: application/json" -d '{"pattern":"[r:[^0-9][0-9]+] [tag:ingredient_measurement_unit] of [tag:ingredient]", "annotationTemplate":"{ingredient:#4.ingredient#, amount:#0#, measure_unit:#2#}"}' "http://localhost:4567/tags/ingredient_with_amount"


OK, now we have a problem: we defined two patterns for the same thing. We want to remove the older one, so let's list the tags and see the rules attached to them:

    curl http://localhost:4567/tags
    curl http://localhost:4567/tags/ingredient_with_amount

the first call list the tags, and the second list the rules for the given one. From there, I see the ids for the old rule, for example _ingredient_with_amount_1_, so with:

    curl -X DELETE -H "Content-Type: application/json"  -d '' "http://localhost:4567/tags/ingredient_with_amount/ingredient_with_amount_3"

this deletes the rule. An important feature of this service is that you can remove rules at runtime, without restarting it.

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
