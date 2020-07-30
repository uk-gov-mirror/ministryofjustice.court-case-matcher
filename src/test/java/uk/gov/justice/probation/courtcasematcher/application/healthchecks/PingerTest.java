package uk.gov.justice.probation.courtcasematcher.application.healthchecks;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.actuate.health.Health;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;

@RunWith(SpringRunner.class)
public class PingerTest {

    private static final String BASE_URL = "http://localhost:8090";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .port(8090)
            .usingFilesUnderClasspath("mocks"));

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