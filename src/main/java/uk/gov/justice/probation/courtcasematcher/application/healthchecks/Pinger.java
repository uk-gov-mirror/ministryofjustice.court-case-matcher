package uk.gov.justice.probation.courtcasematcher.application.healthchecks;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@NoArgsConstructor
public class Pinger {

    @Value("${health.default-ping-path}")
    private String defaultPath;

    public Mono<Health> ping(WebClient webClient) {
        return ping(webClient, defaultPath);
    }

    public Mono<Health> ping(WebClient webClient, String pingPath) {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(pingPath)
                        .build()
                )
                .exchange()
                .map(response -> {
                    if(response.statusCode().is2xxSuccessful()) {
                        return new Health.Builder().up().build();
                    } else {
                        return Health.down().withDetail("httpStatus", response.statusCode().toString()).build();
                    }

                })
                .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()));
    }
}
