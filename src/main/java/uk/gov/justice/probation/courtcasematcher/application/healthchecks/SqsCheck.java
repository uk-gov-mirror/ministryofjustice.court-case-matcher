package uk.gov.justice.probation.courtcasematcher.application.healthchecks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.service.SqsService;

@Component
@Profile("sqs-messaging")
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
