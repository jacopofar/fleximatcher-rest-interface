#!/usr/bin/env bash
echo "this is a showcase for the Fleximatcher REST service. It uses Docker to run the parsing server and WordNet as a service"
echo "stopping the services in case they were running..."
docker stop fleximatcher_rest
docker stop worndet_as_a_service

docker rm fleximatcher_rest
docker rm worndet_as_a_service

echo "starting the Fleximatcher Rest Interface container..."

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

sleep 15

#label 'water' as [tag:ingredient]
curl -X POST -H "Content-Type: application/json"  -d '{"pattern":"water", "annotationTemplate":"{ingredient:\"water\"}"}' "http://localhost:4567/tags/ingredient"

#label 'milk' and 'sugar' as [tag:ingredient]
curl -X POST -H "Content-Type: application/json"  -w "\n" -d '{"pattern":"milk", "annotationTemplate":"{ingredient:\"milk\"}"}' "http://localhost:4567/tags/ingredient"
curl -X POST -H "Content-Type: application/json"  -w "\n" -d '{"pattern":"sugar", "annotationTemplate":"{ingredient:\"sugar\"}"}' "http://localhost:4567/tags/ingredient"

#label 'a litre of something' as [tag:ingredient_with_amount] including the amount in the annotation
curl -X POST -H "Content-Type: application/json"  -w "\n" -d '{"pattern":"a litre of [tag:ingredient]", "annotationTemplate":"{ingredient:#1.ingredient#, amount:\"1\", measure_unit:\"liters\"}"}' "http://localhost:4567/tags/ingredient_with_amount"

#same goes for spoons
curl -X POST -H "Content-Type: application/json"  -w "\n" -d '{"pattern":"a spoon of of [tag:ingredient]", "annotationTemplate":"{ingredient:#1.ingredient#, amount:\"1\", measure_unit:\"spoons\"}"}' "http://localhost:4567/tags/ingredient_with_amount"

#a number of spoons or liters
curl -X POST -H "Content-Type: application/json"  -w "\n" -d '{"pattern":"[r:[0-9]+] litres of [tag:ingredient]", "annotationTemplate":"{ingredient:#2.ingredient#, amount:#0#, measure_unit:\"liters\"}"}' "http://localhost:4567/tags/ingredient_with_amount"
curl -X POST -H "Content-Type: application/json"  -w "\n" -d '{"pattern":"[r:[0-9]+] spoons of [tag:ingredient]", "annotationTemplate":"{ingredient:#2.ingredient#, amount:#0#, measure_unit:\"spoons\"}"}' "http://localhost:4567/tags/ingredient_with_amount"

#show some matches

curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"text":"There'"'"'s milk there","pattern":"[tag:ingredient]"}' "http://localhost:4567/parse"
curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"text":"There'"'"'s a litre of milk there","pattern":"[tag:ingredient_with_amount]"}' "http://localhost:4567/parse"

#generate a few samples
for n in {1..10}; do
  curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"pattern":"[tag:ingredient_with_amount]"}' "http://localhost:4567/generate"
done