#!/usr/bin/env bash
echo "this is a showcase for the Fleximatcher REST service. It uses Docker to run the parsing server and Wordnet as a service"
echo "starting the Fleximatecher Rest Interface container..."
if docker run --name=fleximatcher_rest -d -p 4567:4567 jacopofar/fleximatcher-rest-interface; then
    echo "FRI service started correctly as a Docker daemon container"
else
    echo "something went wrong starting the container. Is Docker installed and can be used by this user?"
fi

echo "starting the Wordnet-as-a-service container..."

if docker run --name=worndet_as_a_service -d -p 5679:5679 jacopofar/wordnet-as-a-service; then
    echo "WordNet-as-a-service service started correctly as a Docker daemon container"
else
    echo "something went wrong starting the container. Is Docker installed and can be used by this user?"
fi
#only for the example, otherwise run curl until the endpoints are up
echo "giving the Docker containers some time to ensure they are listening..."

sleep 10

#label 'water' as [tag:ingredient]
curl -X POST -H "Content-Type: application/json"  -d '{"pattern":"water", "annotationTemplate":"{ingredient:\"water\"}"}' "http://localhost:4567/tags/ingredient"

#label 'milk' as [tag:ingredient]
curl -X POST -H "Content-Type: application/json"  -d '{"pattern":"milk", "annotationTemplate":"{ingredient:\"milk\"}"}' "http://localhost:4567/tags/ingredient"

#label 'a litre of something' as [tag:ingredient] including the amount in the annotation
curl -X POST -H "Content-Type: application/json"  -d '{"pattern":"a litre of [tag:ingredient]", "annotationTemplate":"{ingredient:#1.ingredient#, amount:\"1\", measure_unit:\"liters\"}"}' "http://localhost:4567/tags/ingredient"

#same goes for spoons
curl -X POST -H "Content-Type: application/json"  -d '{"pattern":"a spoon of of [tag:ingredient]", "annotationTemplate":"{ingredient:#1.ingredient#, amount:\"1\", measure_unit:\"spoons\"}"}' "http://localhost:4567/tags/ingredient"

#a number of spoons or liters
curl -X POST -H "Content-Type: application/json"  -d '{"pattern":"[r:[0-9]+] litres of [tag:ingredient]", "annotationTemplate":"{ingredient:#2.ingredient#, amount:#0#, measure_unit:\"liters\"}"}' "http://localhost:4567/tags/ingredient"
curl -X POST -H "Content-Type: application/json"  -d '{"pattern":"[r:[0-9]+] spoons of [tag:ingredient]", "annotationTemplate":"{ingredient:#2.ingredient#, amount:#0#, measure_unit:\"spoons\"}"}' "http://localhost:4567/tags/ingredient"

