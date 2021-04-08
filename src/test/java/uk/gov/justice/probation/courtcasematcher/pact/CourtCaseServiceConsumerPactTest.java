package uk.gov.justice.probation.courtcasematcher.pact;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "court-case-service")
@PactDirectory(value = "build/pacts")
class CourtCaseServiceConsumerPactTest {

    private static final String BASE_MOCK_PATH = "src/test/resources/mocks/__files/";

    private Map<String, String> responseHeaders = new HashMap<>(1);

    @BeforeEach
    void beforeAll() {
        responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", "application/json");
    }

    @Pact(provider="court-case-service", consumer="court-case-matcher")
    public RequestResponsePact getCourtCasePact(PactDslWithProvider builder) throws IOException {

        String body = FileUtils.readFileToString(new File(BASE_MOCK_PATH + "get-court-case/GET_court_case_response_1600028913.json"), UTF_8);

        return builder
            .given("a case exists for court B10JQ and case number 1600028913")
            .uponReceiving("a request for a case by case number")
            .path("/court/B10JQ/case/1600028913")
            .method("GET")
            .willRespondWith()
            .headers(responseHeaders)
            .body(body)
            .status(200)
            .toPact();
    }

    @Pact(provider="court-case-service", consumer="court-case-matcher")
    public RequestResponsePact putCourtCasePact(PactDslWithProvider builder) {

        PactDslJsonBody addressPart = new PactDslJsonBody()
            .stringType("line1", "line2", "line3", "line4", "line5", "postcode");
        PactDslJsonBody namePart = new PactDslJsonBody()
            .stringType("title", "forename1", "forename2", "forename3", "surname");

        PactDslJsonBody body = new PactDslJsonBody()
            .stringValue("courtCode", "B10JQ")
            .stringValue("caseNo", "1600028914")
            .stringType("caseId", "courtRoom", "probationStatus", "defendantName", "defendantSex", "crn", "pnc", "cro", "listNo", "nationality1", "nationality2")
            .booleanType("suspendedSentenceOrder", "breach", "preSentenceActivity")
            .date("previouslyKnownTerminationDate","yyyy-MM-dd")
            .date("defendantDob","yyyy-MM-dd")
            .datetime("sessionStartTime")
            .stringValue("defendantType", "PERSON")
            .object("defendantAddress", addressPart)
            .object("name", namePart)
            .eachLike("offences")
                .stringType("offenceTitle","offenceSummary", "act")
            ;

        return builder
            .given("a case does not exist for court B10JQ and case number 1600028914")
            .uponReceiving("a request to save a new case")
            .body(body)
            .headers("Accept", MediaType.APPLICATION_JSON_VALUE)
            .path("/court/B10JQ/case/1600028914")
            .method("PUT")
            .willRespondWith()
            .headers(responseHeaders)
            .status(201)
            .toPact();
    }

    @Pact(provider="court-case-service", consumer="court-case-matcher")
    public RequestResponsePact postGroupedOffenderMatchesPact(PactDslWithProvider builder) throws IOException {

        String body = FileUtils.readFileToString(new File(BASE_MOCK_PATH + "/post-matches/POST_matches.json"), UTF_8);

        responseHeaders.put("Location", "/court/B10JQ/case/1600028913/grouped-offender-matches/1234");

        return builder
            .given("a case does not exist with grouped offender matches")
            .uponReceiving("a request to create grouped offender matches")
            .body(body)
            .path("/court/B10JQ/case/1600028913/grouped-offender-matches")
            .method("POST")
            .willRespondWith()
            .headers(responseHeaders)
            .status(201)
            .toPact();
    }

    @PactTestFor(pactMethod = "getCourtCasePact")
    @Test
    void getCourtCase(MockServer mockServer) throws IOException {
        var httpResponse = Request
            .Get(mockServer.getUrl() + "/court/B10JQ/case/1600028913")
            .execute()
            .returnResponse();

        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
    }

    @PactTestFor(pactMethod = "putCourtCasePact")
    @Test
    void putCourtCase(MockServer mockServer) throws IOException {
        var httpResponse = Request
            .Put(mockServer.getUrl() + "/court/B10JQ/case/1600028914")
            .setHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .bodyFile(new File(BASE_MOCK_PATH + "PUT_case_details_body.json"), ContentType.APPLICATION_JSON)
            .execute()
            .returnResponse();

        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @PactTestFor(pactMethod = "postGroupedOffenderMatchesPact")
    @Test
    void postGroupedOffenderMatches(MockServer mockServer) throws IOException {
        var httpResponse = Request
            .Post(mockServer.getUrl() + "/court/B10JQ/case/1600028913/grouped-offender-matches")
            .bodyFile(new File(BASE_MOCK_PATH + "/post-matches/POST_matches.json"), ContentType.APPLICATION_JSON)
            .execute()
            .returnResponse();

        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        var locationHeader = Arrays.stream(httpResponse.getHeaders("Location")).findFirst();
        assertThat(locationHeader.get().getValue()).isEqualTo("/court/B10JQ/case/1600028913/grouped-offender-matches/1234");
    }
}
