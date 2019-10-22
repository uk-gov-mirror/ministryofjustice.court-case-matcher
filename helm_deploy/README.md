
### Example deploy command
```
helm --namespace court-probation-dev --tiller-namespace court-probation-dev upgrade court-case-matcher ./court-case-matcher/ --install --values=values-dev.yaml --values=example-secrets.yaml
```

### Rolling back a release
Find the revision number for the deployment you want to roll back:
```
helm --tiller-namespace court-probation-dev history court-case-matcher -o yaml
```
(note, each revision has a description which has the app version and circleci build URL)

Rollback
```
helm --tiller-namespace court-probation-dev rollback court-case-matcher [INSERT REVISION NUMBER HERE] --wait
```

### Helm init

```
helm init --tiller-namespace court-probation-dev --uk.gov.digital.justice.service-account tiller --history-max 200
```
```
