package uk.gov.justice.probation.courtcasematcher.application.healthchecks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;

@ExtendWith(SpringExtension.class)
public class PingerTest {

    private static final String BASE_URL = "http://localhost:8090";

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @Test
    public void when200_thenUp() {
        Pinger pinger = new Pinger("/ping");
        WebClient webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .build();

        Health health = pinger.ping(webClient)
                .block();

        assertThat(health.getStatus()).isEqualTo(UP);
    }

    @Test
    public void when500_thenDown() {
        Pinger pinger = new Pinger("/pingbad");
        WebClient webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .build();

        Health health = pinger.ping(webClient)
                .block();

        assertThat(health.getStatus()).isEqualTo(DOWN);
        assertThat(health.getDetails().get("httpStatus")).isEqualTo("500 INTERNAL_SERVER_ERROR");
    }

    @Test
    public void whenError_thenDown() {
        Pinger pinger = new Pinger("/ping");
        WebClient webClient = WebClient.builder()
                .baseUrl("http://notarealhost")
                .build();

        Health health = pinger.ping(webClient)
                .block();

        assertThat(health.getStatus()).isEqualTo(DOWN);
        assertThat(health.getDetails().get("error")).asString().contains("UnknownHostException");
    }
}
