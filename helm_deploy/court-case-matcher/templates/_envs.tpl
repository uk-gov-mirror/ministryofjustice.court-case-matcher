    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "{{ .Values.spring.profile }}"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: OFFENDER_SEARCH_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-secrets
        key: nomis-oauth-client-id

  - name: APPINSIGHTS_INSTRUMENTATIONKEY
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-secrets
        key: APPINSIGHTS_INSTRUMENTATIONKEY

  - name: OFFENDER_SEARCH_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-secrets
        key: nomis-oauth-client-secret

  - name: GATEWAY_JMS_USERNAME
    valueFrom:
      secretKeyRef:
        name: pict-cpmg-wildfly-credentials
        key: jmsuser

  - name: GATEWAY_JMS_PASSWORD
    valueFrom:
      secretKeyRef:
        name: pict-cpmg-wildfly-credentials
        key: user-password

  - name: AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: crime-portal-gateway-queue-credentials
        key: access_key_id

  - name: AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: crime-portal-gateway-queue-credentials
        key: secret_access_key

  - name: AWS_SQS_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: crime-portal-gateway-queue-credentials
        key: sqs_name

  - name: AWS_SQS_ENDPOINT_URL
    valueFrom:
      secretKeyRef:
        name: crime-portal-gateway-queue-credentials
        key: sqs_id
{{- end -}}
