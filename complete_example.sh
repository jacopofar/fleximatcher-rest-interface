#!/usr/bin/env bash
echo "this is a showcase for the Fleximatcher REST service. It uses Docker to run the parsing server and WordNet as a service"
echo "stopping the services in case they were running..."
docker stop fleximatcher_rest
docker stop worndet_as_a_service

docker rm fleximatcher_rest
docker rm worndet_as_a_service

echo "starting the Wordnet-as-a-service container..."

if docker run --name=wordnet_as_a_service -d -p 5679:5679 jacopofar/wordnet-as-a-service; then
    echo "WordNet-as-a-service service started correctly as a Docker daemon container"
else
    echo "something went wrong starting the container. Is Docker installed and can be used by this user?"
fi

echo "starting the Fleximatcher Rest Interface container..."

if docker run --name=fleximatcher_rest --link wordnet_as_a_service:waas -d -p 4567:4567 jacopofar/fleximatcher-rest-interface; then
    echo "FRI service started correctly as a Docker daemon container"
else
    echo "something went wrong starting the container. Is Docker installed and can be used by this user?"
fi

echo "giving the Docker containers some time to ensure they are running and listening..."

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

#show some matches up to now

curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"text":"There'"'"'s milk there","pattern":"[tag:ingredient]"}' "http://localhost:4567/parse"
curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"text":"There'"'"'s a litre of milk there","pattern":"[tag:ingredient_with_amount]"}' "http://localhost:4567/parse"

#a smarter thing: define a measurement unit tag (singular and plural)
curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"pattern":"litre","annotationTemplate":"{measure_unit:\"litre\"}"}' "http://localhost:4567/tags/ingredient_measurement_unit"
curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"pattern":"spoon","annotationTemplate":"{measure_unit:\"spoon\"}"}' "http://localhost:4567/tags/ingredient_measurement_unit"
curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"pattern":"glass","annotationTemplate":"{measure_unit:\"glass\"}"}' "http://localhost:4567/tags/ingredient_measurement_unit"

curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"pattern":"litres","annotationTemplate":"{measure_unit:\"litre\"}"}' "http://localhost:4567/tags/ingredient_measurement_unit"
curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"pattern":"spoons","annotationTemplate":"{measure_unit:\"spoon\"}"}' "http://localhost:4567/tags/ingredient_measurement_unit"
curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"pattern":"glasses","annotationTemplate":"{measure_unit:\"glass\"}"}' "http://localhost:4567/tags/ingredient_measurement_unit"

#delete the first ones

curl -X DELETE -H "Content-Type: application/json"  -d '' "http://localhost:4567/tags/ingredient_with_amount/ingredient_with_amount_3"
curl -X DELETE -H "Content-Type: application/json"  -d '' "http://localhost:4567/tags/ingredient_with_amount/ingredient_with_amount_4"

#...and use them
curl -X POST -H "Content-Type: application/json" -d '{"pattern":"[r:[^0-9][0-9]+] [tag:ingredient_measurement_unit] of [tag:ingredient]", "annotationTemplate":"{ingredient:#4.ingredient#, amount:#0#, measure_unit:#2#}"}' "http://localhost:4567/tags/ingredient_with_amount"

#generate a few samples
for n in {1..10}; do
  curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"pattern":"[tag:ingredient_with_amount]"}' "http://localhost:4567/generate"
done

#now bind the WordNet HTTP to the service
curl -X PUT -H "Content-Type: application/json" -w "\n"  -d '{"endpoint":"http://waas:5679/hyponym/12"}' "http://localhost:4567/rules/en-hypo"

#show the usage
curl -X POST -H "Content-Type: application/json"  -w "\n"  -d '{"text":"a carrot, a lemon, a lion","pattern":"[en-hypo:w=vegetable]"}' "http://localhost:4567/parse"

#use hyponyms of vegetables and fruits as ingredients

curl -X POST -H "Content-Type: application/json"  -w "\n" -d '{"pattern":"[en-hypo:w=vegetable]", "annotationTemplate":"{ingredient:#0#}"}' "http://localhost:4567/tags/ingredient"
curl -X POST -H "Content-Type: application/json"  -w "\n" -d '{"pattern":"[en-hypo:w=fruit]", "annotationTemplate":"{ingredient:#0#}"}' "http://localhost:4567/tags/ingredient"

#show the usage
curl -X POST -H "Content-Type: application/json" -w "\n" -d '{"text":"a carrot, a lemon, a lion","pattern":"[tag:ingredient]"}' "http://localhost:4567/parse"