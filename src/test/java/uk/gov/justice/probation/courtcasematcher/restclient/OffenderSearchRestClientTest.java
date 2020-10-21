package uk.gov.justice.probation.courtcasematcher.restclient;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient.EXCEPTION_RETRY_FILTER;

@ExtendWith(MockitoExtension.class)
class OffenderSearchRestClientTest {

    private static final HttpHeaders HEADERS = new HttpHeaders(new LinkedMultiValueMap<>());

    @InjectMocks
    private OffenderSearchRestClient restClient;

    @DisplayName("Will not retry for these HTTP status codes")
    @ParameterizedTest(name = "{index} => status=''{0}''")
    @EnumSource(value = HttpStatus.class, names = {"UNAUTHORIZED", "NOT_FOUND", "FORBIDDEN", "TOO_MANY_REQUESTS"})
    void givenUnauthorizedError_whenUsePredicate_thenNoRetry(HttpStatus httpStatus) {

        WebClientResponseException exception = createException(httpStatus);

        assertThat(EXCEPTION_RETRY_FILTER.test(exception)).isFalse();
    }

    @DisplayName("Will retry for these HTTP status codes (and lots of others)")
    @ParameterizedTest(name = "{index} => status=''{0}''")
    @EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "REQUEST_TIMEOUT"})
    void givenHttpStatusToRetry_whenUsePredicate_thenRetry(HttpStatus httpStatus) {
        WebClientResponseException exception = createException(httpStatus);

        assertThat(EXCEPTION_RETRY_FILTER.test(exception)).isTrue();
    }

    @DisplayName("The mono will be an error with invalid name")
    @Test
    void givenInvalidName_whenSearch_thenReturnError() {
        Name name = Name.builder().forename1("not").surname("").build();
        Mono<SearchResponse> searchResponseMono = restClient.search(name, LocalDate.of(1999, 4, 5));

        StepVerifier.create(searchResponseMono)
            .verifyError(IllegalArgumentException.class);
    }

    @DisplayName("The mono will be an error with null date of birth")
    @Test
    void givenNullDateOfBirth_whenSearch_thenReturnError() {
        Name name = Name.builder().forename1("Arthur").surname("MORGAN").build();
        Mono<SearchResponse> searchResponseMono = restClient.search(name, null);

        StepVerifier.create(searchResponseMono)
            .verifyError(IllegalArgumentException.class);
    }

    private static WebClientResponseException createException(HttpStatus httpStatus) {
        return WebClientResponseException.create(httpStatus.value(), "", HEADERS, "".getBytes(), null);
    }
}
