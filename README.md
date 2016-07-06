Fleximatecher HTTP interface
============================

A parser/annotator completely accessible through a REST interface, allowing grammar rules to be changed on the fly.

It lets you:

* match substrings and have partial matches
* enrich the text with arbitrary annotations
* define your own annotators/grammar rules as HTTP services (WordNet, etc.)
* get explanations for a parsing result
* generate text for a given pattern
* add, delete and list the grammar rules and the annotators at runtime
* work on UTF-8 text

and it's already on Docker Hub.

How to use
----------
In this example, we'll build a toy parser to identify room reservation requests.

TODO write it


Roadmap
-------

The following functions are to be implemented:

* parse a text and return the corresponding annotations ✓
* add new annotations ✓
* define annotations using HTTP ✓
* generate text from patterns ✓
* explicitly explain all the steps leading to a parse result ✓
* give said steps in a human-readable report (not a JSON) ☐
* export and import all the rules in a single file ✓
* return statistics on the average parse duration and the usage of the different tags ☐
* define parsers using Javascript ☐
