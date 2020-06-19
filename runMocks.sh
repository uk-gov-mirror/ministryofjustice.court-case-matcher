#!/bin/sh
cd ./src/test/resources/mocks/
docker image build -t court-case-matcher-mocks:local .
docker run -p 8090:8080 -it court-case-matcher-mocks:local --verbose