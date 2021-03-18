#!/usr/bin/env bash
set -e
export TERM=ansi
export AWS_ACCESS_KEY_ID=foobar
export AWS_SECRET_ACCESS_KEY=foobar
export AWS_DEFAULT_REGION=eu-west-2
export PAGER=

aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name crime-portal-gateway-queue-dlq
aws --endpoint-url http://localhost:4566 sqs create-queue --queue-name crime-portal-gateway-queue

aws --endpoint-url=http://localhost:4566 sqs set-queue-attributes --queue-url "http://localhost:4566/queue/crime-portal-gateway-queue" --attributes '{"RedrivePolicy":"{\"maxReceiveCount\":\"1\", \"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:crime-portal-gateway-queue-dlq\"}"}'

echo "SQS Configured"
