package uk.gov.justice.probation.courtcasematcher.application.healthchecks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component("courtCaseServiceHealthIndicator")
public class CourtCaseService implements ReactiveHealthIndicator {
    @Autowired
    @Qualifier("courtCaseServiceWebClient")
    private WebClient courtCaseServiceWebClient;
    @Autowired
    private Pinger pinger;

    @Override
    public Mono<Health> health() {
        return pinger.ping(courtCaseServiceWebClient);
    }
}