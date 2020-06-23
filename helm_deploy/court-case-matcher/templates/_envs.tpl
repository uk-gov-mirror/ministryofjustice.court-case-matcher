    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "dev"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: OFFENDER_SEARCH_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-secrets
        key: nomis-oauth-client-id

  - name: OFFENDER_SEARCH_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-secrets
        key: nomis-oauth-client-secret
{{- end -}}
