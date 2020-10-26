package uk.gov.justice.probation.courtcasematcher.restclient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import com.google.common.eventbus.EventBus;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.Retry.RetrySignal;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseSuccessEvent;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.OffenderDetail;
import uk.gov.justice.probation.courtcasematcher.restclient.exception.CourtCaseNotFoundException;
import uk.gov.justice.probation.courtcasematcher.restclient.exception.CourtNotFoundException;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;
import static uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient.EXCEPTION_RETRY_FILTER;

@Component
@Slf4j
public class CourtCaseRestClient {

    private static final String ERR_MSG_FORMAT_PUT_ABSENT = "Unexpected exception when applying PUT to purge absent cases for court '%s'";
    private static final String ERR_MSG_FORMAT_PUT_CASE = "Unexpected exception when applying PUT to update case number '%s' for court '%s'.";
    private static final String ERR_MSG_FORMAT_POST_MATCH = "Unexpected exception when POST matches for case number '%s' for court '%s'. Match count was %s";

    private static final String ERROR_MSG_FORMAT_INITIAL_CASE = "Initial error from PUT of the court case. Will retry.";
    private static final String ERROR_MSG_FORMAT_RETRY_PUT_CASE = "Retry error from PUT of the court case, at attempt %s of %s.";
    private static final String ERROR_MSG_FORMAT_INITIAL_MATCHES = "Initial error from POST of the offender matches. Will retry.";
    private static final String ERROR_MSG_FORMAT_RETRY_POST_MATCHES = "Retry error from POST of the offender matches, at attempt %s of %s.";

    @Value("${court-case-service.case-put-url-template}")
    private String courtCasePutTemplate;
    @Value("${court-case-service.matches-post-url-template}")
    private String matchesPostTemplate;
    @Value("${court-case-service.purge-absent-put-url-template}")
    private String purgeAbsentPutTemplate;
    @Value("${court-case-service.offender-detail-get-url-template}")
    private String offenderDetailGetTemplate;

    @Value("${court-case-service.disable-authentication:false}")
    private Boolean disableAuthentication;

    private final EventBus eventBus;

    private final WebClient webClient;

    @Setter
    @Value("${court-case-service.max-retries:3}")
    private int maxRetries;

    @Setter
    @Value("${court-case-service.min-backoff-seconds:3}")
    private int minBackOffSeconds;

    @Autowired
    public CourtCaseRestClient(@Qualifier("courtCaseServiceWebClient") WebClient webClient, EventBus eventBus) {
        super();
        this.webClient = webClient;
        this.eventBus = eventBus;
    }

    public Mono<CourtCase> getCourtCase(final String courtCode, final String caseNo) throws WebClientResponseException {
        final String path = String.format(courtCasePutTemplate, courtCode, caseNo);

        // Get the existing case. Not a problem if it's not there. So return a Mono.empty() if it's not
        return get(path)
            .retrieve()
            .onStatus(HttpStatus::isError, (clientResponse) -> handleGetError(clientResponse, courtCode, caseNo))
            .bodyToMono(CourtCase.class)
            .map(courtCaseResponse -> {
                log.debug("GET succeeded for retrieving the case for path {}", path);
                return courtCaseResponse;
            })
            .onErrorResume((e) -> {
                // This is normal in the context of CCM, don't log
                return Mono.empty();
            });
    }

    public Disposable putCourtCase(String courtCode, String caseNo, CourtCase courtCase) {
        final String path = String.format(courtCasePutTemplate, courtCode, caseNo);
        final GroupedOffenderMatches offenderMatches = courtCase.getGroupedOffenderMatches();

        return put(path, courtCase)
            .retrieve()
            .bodyToMono(CourtCase.class)
            .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(minBackOffSeconds))
                .jitter(0.0d)
                .doAfterRetryAsync((retrySignal) -> logRetrySignal(retrySignal, ERROR_MSG_FORMAT_RETRY_PUT_CASE, ERROR_MSG_FORMAT_INITIAL_CASE))
                .filter(EXCEPTION_RETRY_FILTER))
            .onErrorResume(this::handleError)
            .subscribe(courtCaseApi -> {
                eventBus.post(CourtCaseSuccessEvent.builder().courtCase(courtCaseApi).build());
                postMatches(courtCase.getCourtCode(), courtCase .getCaseNo(), offenderMatches);
            }, throwable -> {
                eventBus.post(CourtCaseFailureEvent.builder()
                    .failureMessage(String.format(ERR_MSG_FORMAT_PUT_CASE, caseNo, courtCode))
                    .throwable(throwable)
                    .build());
                postMatches(courtCase.getCourtCode(), courtCase .getCaseNo(), offenderMatches);
            });
    }

    public void postMatches(String courtCode, String caseNo, GroupedOffenderMatches offenderMatches) {

        Optional.ofNullable(offenderMatches).ifPresent(matches -> {
            final String path = String.format(matchesPostTemplate, courtCode, caseNo);
            post(path, matches)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(minBackOffSeconds))
                    .jitter(0.0d)
                    .doAfterRetryAsync((retrySignal) -> logRetrySignal(retrySignal, ERROR_MSG_FORMAT_RETRY_POST_MATCHES, ERROR_MSG_FORMAT_INITIAL_MATCHES))
                    .filter(EXCEPTION_RETRY_FILTER))
                .subscribe(responseEntity -> {
                    log.info("Successful POST of offender matches. Response location: {} ",
                        Optional.ofNullable(responseEntity.getHeaders().getFirst(HttpHeaders.LOCATION))
                            .orElse("[NOT FOUND]"));
                }, throwable -> {
                    String error = String.format(ERR_MSG_FORMAT_POST_MATCH, courtCode, caseNo, offenderMatches.getMatches().size());
                    log.error(error, throwable);
                });
        });
    }

    public void purgeAbsent(String courtCode, Map<LocalDate, List<String>> cases) {

        final String path = String.format(purgeAbsentPutTemplate, courtCode);
        put(path, cases)
            .retrieve()
            .onStatus(HttpStatus::isError, (clientResponse) -> handleError(clientResponse, () -> new CourtNotFoundException(courtCode)))
            .toBodilessEntity()
            .doOnError(e -> log.error(String.format(ERR_MSG_FORMAT_PUT_ABSENT, courtCode) + ".Exception : " + e))
            .subscribe(responseEntity -> {
                log.info("Successful PUT of all cases for purge in court case service for court {}", courtCode);
            });
    }

    public Mono<String> getProbationStatus(String crn) {
        final String path = String.format(offenderDetailGetTemplate, crn);

        // Get the existing case. Not a problem if it's not there. So return a Mono.empty() if it's not
        return get(path)
            .retrieve()
            .bodyToMono(OffenderDetail.class)
            .map(offenderDetail -> {
                log.info("GET succeeded for retrieving the offender probation status for path {}", path);
                return offenderDetail.getProbationStatus();
            })
            .onErrorResume((e) -> {
                log.info("GET failed for retrieving the offender probation status for path {}. Will return empty status", path, e);
                return Mono.just("");
            });
    }

    private WebClient.RequestHeadersSpec<?> get(String path) {
        final WebClient.RequestHeadersSpec<?> spec = webClient
            .get()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .accept(MediaType.APPLICATION_JSON);

        return addSpecAuthAttribute(spec, path);
    }

    private WebClient.RequestHeadersSpec<?> put(String path, CourtCase courtCase) {
        WebClient.RequestHeadersSpec<?> spec =  webClient
            .put()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .body(Mono.just(courtCase), CourtCase.class)
            .accept(MediaType.APPLICATION_JSON);

        return addSpecAuthAttribute(spec, path);
    }

    private WebClient.RequestHeadersSpec<?> put(String path, Map<LocalDate, List<String>> casesByDate) {
        var spec = webClient
            .put()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .body(Mono.just(casesByDate), Map.class)
            .accept(MediaType.APPLICATION_JSON);

        return addSpecAuthAttribute(spec, path);
    }

    private WebClient.RequestHeadersSpec<?> post(String path, GroupedOffenderMatches request) {
        WebClient.RequestHeadersSpec<?> spec = webClient
            .post()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .body(Mono.just(request), GroupedOffenderMatches.class)
            .accept(MediaType.APPLICATION_JSON);

        return addSpecAuthAttribute(spec, path);
    }

    private RequestHeadersSpec<?> addSpecAuthAttribute(RequestHeadersSpec<?> spec, String path) {
        if (disableAuthentication) {
            return spec;
        }

        log.info(String.format("Authenticating with %s for call to %s", "offender-search-client", path));
        return spec.attributes(clientRegistrationId("offender-search-client"));
    }

    private Mono<? extends CourtCase> handleError(Throwable throwable) {

        if (Exceptions.isRetryExhausted(throwable)) {
            log.error(String.format(ERROR_MSG_FORMAT_RETRY_PUT_CASE, maxRetries, maxRetries));
        }
        return Mono.error(throwable);
    }

    private Mono<? extends Throwable> handleGetError(ClientResponse clientResponse, String courtCode, String caseNo) {
        final HttpStatus httpStatus = clientResponse.statusCode();
        // This is expected for new cases
        if (HttpStatus.NOT_FOUND.equals(httpStatus)) {
            log.info("Failed to get case for case number {} and court code {}", caseNo, courtCode);
            return Mono.error(new CourtCaseNotFoundException(courtCode, caseNo));
        }
        else if(HttpStatus.UNAUTHORIZED.equals(httpStatus) || HttpStatus.FORBIDDEN.equals(httpStatus)) {
            log.error("HTTP status {} to to GET the case from court case service", httpStatus);
        }
        throw WebClientResponseException.create(httpStatus.value(),
            httpStatus.name(),
            clientResponse.headers().asHttpHeaders(),
            clientResponse.toString().getBytes(),
            StandardCharsets.UTF_8);
    }

    private Mono<? extends Throwable> handleError(ClientResponse clientResponse, Supplier<Throwable> notFoundError) {
        final HttpStatus httpStatus = clientResponse.statusCode();
        if (HttpStatus.NOT_FOUND.equals(httpStatus)) {
            return Mono.error(notFoundError.get());
        }
        throw WebClientResponseException.create(httpStatus.value(),
            httpStatus.name(),
            clientResponse.headers().asHttpHeaders(),
            clientResponse.toString().getBytes(),
            StandardCharsets.UTF_8);
    }

    private Mono<Void> logRetrySignal(RetrySignal retrySignal, String messageFormat, String initialError) {
        if (retrySignal.totalRetries() > 0 ) {
            log.error(String.format(messageFormat, retrySignal.totalRetries(), maxRetries));
        }
        else {
            log.error(initialError);
        }
        return Mono.empty();
    }
}
