package uk.gov.justice.probation.courtcasematcher.application.healthchecks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.service.SqsService;

@Component
@ConditionalOnProperty(value="messaging.sqs.enabled", havingValue = "true")
public class SqsCheck implements ReactiveHealthIndicator {

    @Autowired
    private SqsService sqsService;

    @Override
    public Mono<Health> health() {
        var health = new Health.Builder().down().build();
        if (sqsService.isQueueAvailable()) {
            health = Health.up().build();
        }
        return Mono.just(health);
    }

}
