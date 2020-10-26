package uk.gov.justice.probation.courtcasematcher.restclient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient.EXCEPTION_RETRY_FILTER;

@ExtendWith(MockitoExtension.class)
class OffenderSearchRestClientTest {

    private static final HttpHeaders HEADERS = new HttpHeaders(new LinkedMultiValueMap<>());

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

    static WebClientResponseException createException(HttpStatus httpStatus) {
        return WebClientResponseException.create(httpStatus.value(), "", HEADERS, "".getBytes(), null);
    }
}
