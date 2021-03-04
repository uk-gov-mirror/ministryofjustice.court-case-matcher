package uk.gov.justice.probation.courtcasematcher.health;

import io.restassured.RestAssured;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.jms.JmsHealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.TestConfig;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.application.healthchecks.SqsCheck;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
public class HealthCheckTest {

    @Autowired
    public ActiveMQConnectionFactory jmsConnectionFactory;

    @Autowired
    public JmsHealthIndicator jmsHealthIndicator;

    @Autowired
    private SqsCheck sqsCheck;

    @LocalServerPort
    private int port;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @BeforeEach
    public void before() {
        TestConfig.configureRestAssuredForIntTest(port);
        RestAssured.basePath = "/actuator";
    }

    @Test
    public void testUp() {
        Mockito.when(jmsHealthIndicator.getHealth(any(Boolean.class))).thenReturn(Health.up().build());
        Mockito.when(sqsCheck.getHealth(any(Boolean.class))).thenReturn(Mono.just(Health.up().build()));

        String response = given()
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .extract().response().asString();

        assertThatJson(response).node("status").isEqualTo("UP");
        assertThatJson(response).node("components.jms.status").isEqualTo("UP");
        assertThatJson(response).node("components.offenderSearch.status").isEqualTo("UP");
        assertThatJson(response).node("components.courtCaseService.status").isEqualTo("UP");
        assertThatJson(response).node("components.nomisAuth.status").isEqualTo("UP");
    }

    @Test
    public void whenJmsDown_thenDownWithStatus503() {
        Mockito.when(jmsHealthIndicator.getHealth(any(Boolean.class))).thenReturn(Health.down().build());
        Mockito.when(sqsCheck.getHealth(any(Boolean.class))).thenReturn(Mono.just(Health.down().build()));
        String response = given()
            .when()
            .get("/health")
            .then()
            .statusCode(503)
            .extract().response().asString();

        assertThatJson(response).node("status").isEqualTo("DOWN");
        assertThatJson(response).node("components.jms.status").isEqualTo("DOWN");
        assertThatJson(response).node("components.offenderSearch.status").isEqualTo("UP");
        assertThatJson(response).node("components.courtCaseService.status").isEqualTo("UP");
        assertThatJson(response).node("components.nomisAuth.status").isEqualTo("UP");
    }

}
