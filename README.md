Court Case Matcher
==================
[![CircleCI](https://circleci.com/gh/ministryofjustice/court-case-matcher.svg?style=svg)](https://circleci.com/gh/ministryofjustice/court-case-matcher)

Service to receive details of cases, incoming from Libra, match against existing cases in court case service, match to offenders (offender-search) and update court case service.

Dev Setup
---------

The service uses Lombok and so annotation processors must be [turned on within the IDE](https://www.baeldung.com/lombok-ide).

court-case-matcher is capable of reading messages from a configured SQS queue. There is a docker compose config which will start SQS services with a correctly configured queue. This is required for running  integration tests which use AWS.

There are integration tests which send messages to the SQS queue. If required, it is also possible to send them via a running instance of crime-portal-gateway, or using the AWS CLI

```
aws sqs send-message --region eu-west-2 --endpoint-url http://localhost:4566  --queue-url http://localhost:4566/000000000000/crime-portal-gateway-queue  --message-body $msg
```

```
docker-compose up localstack
```

### Environment 

The following environment variables should be set when running the spring boot application, so as to enable communications with offender-search. The secret can be looked up from "court-case-matcher-secrets".

```
offender-search-client-secret=[insert secret string here]
offender-search-client-id=court-case-matcher
```

### Application health
```
curl -X GET http://localhost:8080/actuator/health
```

Spring Boot exposes liveness and readiness probes at the following endpoints

```
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

### Application Logs

Spring Boot exposes logging levels in preprod and dev profiles and they may be changed at runtime. 
For example, logging level can be set to TRACE to expose the raw messages as they are received from 
the messaging queue.

To view all logging levels 

```
https://court-case-matcher-dev.apps.live-1.cloud-platform.service.justice.gov.uk/actuator/loggers
```

To alter the level of the MessageReceiver to TRACE.

```
curl -i -X POST -H 'Content-Type: application/json' -d '{"configuredLevel": "TRACE"}' https://court-case-matcher-dev.apps.live-1.cloud-platform.service.justice.gov.uk/actuator/loggers/uk.gov.justice.probation.courtcasematcher.messaging.MessageReceiver
```

### Application Ping
```
curl -X GET http://localhost:8080/ping
```


### Feature Flags

The following feature flags are in operation.

| Name      | Description |
| ----------- | ----------- |


