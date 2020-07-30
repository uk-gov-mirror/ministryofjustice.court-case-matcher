package uk.gov.justice.probation.courtcasematcher.application.healthchecks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class OffenderSearch implements ReactiveHealthIndicator {
    @Autowired
    @Qualifier("offenderSearchWebClient")
    private WebClient offenderSearchWebClient;

    @Autowired
    private Pinger pinger;

    @Value("${offender-search.ping-url}")
    private String path;

    @Override
    public Mono<Health> health() {
        return pinger.ping(offenderSearchWebClient, path);
    }
}