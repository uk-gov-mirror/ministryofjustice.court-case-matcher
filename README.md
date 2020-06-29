Court Case Matcher
==================
Service to receive details of cases, incoming from Libra, match against existing cases in court case service, match to offenders (offender-search) and update court case service.

Dev Setup
---------

The service uses Lombok and so annotation processors must be [turned on within the IDE](https://www.baeldung.com/lombok-ide).

In order to run the matcher and achieve messages incoming on a developer's system, we need the crime portal mirror gateway (CPMG) running. This component is able to receive SOAP messages, decodes them and places them on an Active MQ queue. 

1. Run Crime Portal Mirror Gateway (https://github.com/ministryofjustice/crime-portal-mirror-gateway). 
We need a version of this where messages are not encrypted and which creates a WildFly instance configured for JMS. This has been altered on a branch named local-dev-run. The project contains a docker-compose configuration which will build and run the WildFly and postgres containers. The steps below show how the container is built, started and finally the application of one modification to WildFly.


        
        git clone git@github.com:ministryofjustice/crime-portal-mirror-gateway.git
        cd crime-portal-mirror-gateway
        git checkout local-dev-run
        
        # Builds the non-encrypted version of the WAR (you need JDK 8 to build)
        ./gradlew clean build
        
        # Builds the docker images (change the passwords and usernames if required)
        docker-compose build --build-arg APP_USERNAME="appuser" --build-arg APP_PASSWORD="appuser" --build-arg JMS_USERNAME="jmsuser" --build-arg JMS_PASSWORD="jmsuser"
        
        # Starts as daemon
        docker-compose up -d
        

2.SOAP UI. (https://www.soapui.org/downloads/soapui/)

CGI have provided a project file which can be used with this software to send SOAP messages. However, the message payloads are not signed which is why we need a special version of CPMG. Download the software (community edition). Open the project XML file found in the "soap-ui-project" folder of the crime portal mirror gateway. This is configured to send the SOAP request to a running CPMG container at port 8080 on "localhost".

If you connect to the postgres database running in the local container, you will see records of the message receipt in the message tables. SOAP UI will also show a successful acknowledgement of the SOAP receipt.

Run court case matcher, but alter the server port because wildfly is also serving on port 8080. Make sure that the jmsuser and jmspassword values in your chosen spring profile match those that you built the wildfly container with.

```env SPRING_PROFILES_ACTIVE=dev SERVER_PORT=8081 ./gradlew bootRun```

The court case matcher will now receive the messages and will output messages like this to the console 

```
2020-06-03 15:24:59.087  INFO 17281 --- [enerContainer-1] u.g.j.p.c.messaging.MessageProcessor     : Received message UUID 09233523-345b-4351-b623-5dsf35sgs5d6, from CP_NPS_ML, original timestamp 2020-06-03T14:14:56.871Z
   2020-06-03 15:24:59.087  INFO 17281 --- [enerContainer-1] u.g.j.p.c.messaging.MessageProcessor     : Received 18 cases
   2020-06-03 15:24:59.087  INFO 17281 --- [enerContainer-1] u.g.j.p.c.messaging.MessageProcessor     : Successfully published case ID: 1217464, case no:1600032952 for defendant: DLONE
   2020-06-03 15:24:59.087  INFO 17281 --- [enerContainer-1] u.g.j.p.c.messaging.MessageProcessor     : Successfully published case ID: 1218461, case no:1600032979 for defendant: DLUNSCHEDULEDLISTONE
   2020-06-03 15:24:59.087  INFO 17281 --- [enerContainer-1] u.g.j.p.c.messaging.MessageProcessor     : Successfully published case ID: 1218462, case no:1600032987 for defendant: DLUNSCHEDULEDLISTTWO
   2020-06-03 15:24:59.087  INFO 17281 --- [enerContainer-1] u.g.j.p.c.messaging.MessageProcessor     : Successfully published case ID: 1218463, case no:1600032995 for defendant: DLUNSCHEDULEDTHREE
   2020-06-03 15:24:59.087  INFO 17281 --- [enerContainer-1] u.g.j.p.c.messaging.MessageProcessor     : Successfully published case ID: 1216463, case no:1600032855 for defendant: Tod TEBAILCONDPENDING
```
### Environment 

The following environment variables should be set when running the spring boot application, so as to enable communications with offender-search.

```
offender-search-client-secret=[insert secret string here]
offender-search-client-id=probation-in-court
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

### Application Ping
```
curl -X GET http://localhost:8080/ping
```
