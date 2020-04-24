# offer-rider search-service

## about
This service manages data flows in system for automatic searching advertisements on e-commerce web sites

## build
To generate fat executable jar use `sbt assembly`. The jar will be a available in `target/scala-2.13/scrapper-search-service-assembly-0.1.jar`.

## run
### jar
To run it use:
```shell script
java -jar target/scala-2.13/scrapper-search-service-assembly-0.1.jar
```

### docker
tba

## integrate
### events
#### new search tasks
The service periodically emits new tasks on `pt-scraper-search-tasks` channel. These events indicate start of new search tasks.

Sent message:
```json
{
   "taskId": "uuid, the id assigned to this task",
   "params": {
      "key1": "textValue",
      "key2": "numberValueAsText"
   }
}
```

#### search tasks results
The service listens for search results on `pt-scraper-results` channel. These events indicates new result entry for task started by earlier event.

Awaited message:
```json
{
    "taskId": "uuid, the id of task this result belongs to",
    "title": "string",
    "subtitle": "string?",
    "url": "string?",
    "imgUrl": "string?",
    "params": {
      "key1": "textValue",
      "key2": "numberValueAsText"
    },
    "last": "bool, true if no further results will be emitted for this taskId"
}
``` 

### REST API
#### endpoints
```
POST /search
GET /search
GET /tasks
GET /results
```

#### postman
[![Run in Postman](https://run.pstmn.io/button.svg)](https://app.getpostman.com/run-collection/7f7fdb04e7e9c1973e26)

The rest api allows clients to see and manage searches, tasks and results. 

To discover and perform sample requests use the Postman collections from `docs/scraper-search-service.postman_collection.json`. In order to run the collections environment variable `HOST` must be defined.

## credits
tba
