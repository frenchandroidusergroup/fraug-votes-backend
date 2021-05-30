A GraphQL server to register votes

* setup credentials: `source setup-env.sh`
* run locally: `./gradlew bootRun`
* debug locally: http://localhost:8080/playground
* build docker image: `./gradlew dockerBuildImage`
* push image to Cloud Registry: `docker tag fraug-votes-backend/server eu.gcr.io/fraug-votes/server && docker push eu.gcr.io/fraug-votes/server`