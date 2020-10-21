package uk.gov.justice.probation.courtcasematcher.restclient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.StringUtils;
import reactor.util.retry.Retry;
import reactor.util.retry.Retry.RetrySignal;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Component
@Slf4j
public class OffenderSearchRestClient {
    private static final String ERROR_NO_DATE_OF_BIRTH = "No dateOfBirth provided";
    private static final String ERROR_NO_NAME = "No surname provided";

    @Setter
    @Value("${offender-search.post-match-url}")
    private String postMatchUrl;

    @Setter
    @Value("${offender-search.disable-authentication:false}")
    private Boolean disableAuthentication;

    @Setter
    @Value("${offender-search.max-retries:3}")
    private int maxRetries;

    @Setter
    @Value("${offender-search.min-backoff-seconds:5}")
    private int minBackOffSeconds;

    private final WebClient webClient;

    @Autowired
    public OffenderSearchRestClient(@Qualifier("offenderSearchWebClient") WebClient webClient) {
        super();
        this.webClient = webClient;
    }

    public Mono<SearchResponse> search(Name name, LocalDate dateOfBirth){
        if (!validate(name, dateOfBirth)) {
            return Mono.error(new IllegalArgumentException("Invalid parameters passed for offender search"));
        }
        MatchRequest body = buildRequestBody(name, dateOfBirth);
        return post()
                .body(BodyInserters.fromPublisher(Mono.just(body), MatchRequest.class))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(SearchResponse.class)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(minBackOffSeconds))
                                .jitter(0.0d)
                                .doAfterRetryAsync(this::logRetrySignal)
                                .filter(EXCEPTION_RETRY_FILTER))
                .onErrorResume(this::handleError)
            ;
    }

    private WebClient.RequestBodySpec post() {
        WebClient.RequestBodySpec postSpec = webClient
                .post()
                .uri(postMatchUrl);

        if (!disableAuthentication)  {
            return postSpec.attributes(clientRegistrationId("offender-search-client"));
        } else {
            return postSpec;
        }
    }

    private boolean validate(Name name, LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            log.error(ERROR_NO_DATE_OF_BIRTH);
            return false;
        }

        if (name == null || StringUtils.isEmpty(name.getSurname())) {
            log.error(ERROR_NO_NAME);
            return false;
        }
        return true;
    }

    private Mono<? extends SearchResponse> handleError(Throwable throwable) {

        if (Exceptions.isRetryExhausted(throwable)) {
            log.error("Retry error :{} with maximum of {}", throwable.getMessage(), maxRetries);
            return Mono.error(throwable);
        }
        return Mono.error(throwable);
    }

    private MatchRequest buildRequestBody(Name fullName, LocalDate dateOfBirth) {

        MatchRequest.MatchRequestBuilder builder = MatchRequest.builder()
                                                    .surname(fullName.getSurname())
                                                    .dateOfBirth(dateOfBirth.format(DateTimeFormatter.ISO_DATE));
        String forenames = fullName.getForenames();
        if (!StringUtils.isEmpty(forenames)) {
            builder.firstName(forenames);
        }
        return builder.build();
    }

    private Mono<Void> logRetrySignal(RetrySignal retrySignal) {
        log.error("Error from call to offender search, at attempt {} of {}. Root Cause {} ",
            retrySignal.totalRetries(), maxRetries, retrySignal.failure());
        return Mono.empty();
    }

    /**
     * Filter which decides whether or not to retry. Return true if we do wish to retry.
     */
    static final Predicate<? super Throwable> EXCEPTION_RETRY_FILTER = new Predicate<Throwable>() {
        @Override
        public boolean test(Throwable throwable) {
            boolean retry = true;
            if (throwable instanceof WebClientResponseException) {
                WebClientResponseException ex = (WebClientResponseException) throwable;
                HttpStatus status = ex.getStatusCode();
                switch (status) {
                    case NOT_FOUND:
                    case FORBIDDEN:
                    case UNAUTHORIZED:
                    case TOO_MANY_REQUESTS:
                        retry = false;
                        break;
                    default:
                        retry = true;
                }
            }

            return retry;
        }
    };
}
