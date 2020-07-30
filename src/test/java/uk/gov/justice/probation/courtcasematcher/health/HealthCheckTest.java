package uk.gov.justice.probation.courtcasematcher.health;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.justice.probation.courtcasematcher.TestConfig;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class HealthCheckTest {

    @LocalServerPort
    int port;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .port(8090)
            .usingFilesUnderClasspath("mocks"));

    @Before
    public void before() {
        TestConfig.configureRestAssuredForIntTest(port);
        RestAssured.basePath = "/actuator";
    }

    @Test
    public void testUp() {

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
}

