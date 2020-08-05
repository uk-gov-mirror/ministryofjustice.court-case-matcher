package uk.gov.justice.probation.courtcasematcher.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
public class PingControllerTest {

    @LocalServerPort
    int port;

    @Before
    public void before() {
        RestAssured.port = port;
        RestAssured.basePath = "/";
    }

    @Test
    public void pingEndpoint() {

        String response = given()
                .when()
                .get("ping")
                .then()
                .statusCode(200)
                .extract().response().asString();

        assertThat(response).isEqualTo("pong");
    }
}
