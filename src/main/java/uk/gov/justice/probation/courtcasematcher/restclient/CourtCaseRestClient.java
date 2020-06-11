package uk.gov.justice.probation.courtcasematcher.restclient;

import com.google.common.eventbus.EventBus;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseSuccessEvent;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.restclient.exception.CourtCaseNotFoundException;

@Component
@Slf4j
public class CourtCaseRestClient {

    private static final String ERROR_MSG_FORMAT = "Unexpected exception when applying PUT to update case number '%s' for court '%s'";
    private static final String PUT_404_MSG_FORMAT = "PUT of case no '%s' for court '%s' failed. Http status '%s'. Likely an incorrect court code.";

    @Value("${court-case-service.case-put-url-template}")
    private String courtCasePutTemplate;

    private final EventBus eventBus;

    private final WebClient webClient;

    @Autowired
    public CourtCaseRestClient(WebClient webClient, EventBus eventBus) {
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
                log.debug("GET failed for retrieving the case for path {}", path);
                return Mono.empty();
            });
    }

    public Disposable putCourtCase(String courtCode, String caseNo, CourtCase courtCase) {
        final String path = String.format(courtCasePutTemplate, courtCode, caseNo);

        return put(path, courtCase)
            .retrieve()
            .onStatus(HttpStatus::isError, (clientResponse) -> handlePutError(clientResponse, courtCode, caseNo))
            .bodyToMono(CourtCase.class)
            .doOnError(e -> postErrorToBus(String.format(ERROR_MSG_FORMAT, caseNo, courtCode) + ".Exception : " + e))
            .subscribe(courtCaseApi -> {
                eventBus.post(CourtCaseSuccessEvent.builder().courtCaseApi(courtCaseApi).build());
            });
    }

    private WebClient.RequestHeadersSpec<?> get(String path) {
        return webClient
            .get()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .accept(MediaType.APPLICATION_JSON);
    }

    private WebClient.RequestHeadersSpec<?> put(String path, CourtCase courtCase) {
        return webClient
            .put()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .body(Mono.just(courtCase), CourtCase.class)
            .accept(MediaType.APPLICATION_JSON);
    }

    private Mono<? extends Throwable> handleGetError(ClientResponse clientResponse, String courtCode, String caseNo) {
        final HttpStatus httpStatus = clientResponse.statusCode();
        // This is expected for new cases
        if (HttpStatus.NOT_FOUND.equals(clientResponse.statusCode())) {
            log.info("Failed to get case for case number {} and court code {}", caseNo, courtCode);
            return Mono.error(new CourtCaseNotFoundException(courtCode, caseNo));
        }
        throw WebClientResponseException.create(httpStatus.value(),
            httpStatus.name(),
            clientResponse.headers().asHttpHeaders(),
            clientResponse.toString().getBytes(),
            StandardCharsets.UTF_8);
    }

    private Mono<? extends Throwable> handlePutError(ClientResponse clientResponse, String courtCode, String caseNo) {
        final HttpStatus httpStatus = clientResponse.statusCode();
        if (HttpStatus.NOT_FOUND.equals(httpStatus)) {
            return Mono.error(new CourtCaseNotFoundException(courtCode, caseNo));
        }
        throw WebClientResponseException.create(httpStatus.value(),
            httpStatus.name(),
            clientResponse.headers().asHttpHeaders(),
            clientResponse.toString().getBytes(),
            StandardCharsets.UTF_8);
    }

    private void postErrorToBus(String failureMessage) {
        eventBus.post(CourtCaseFailureEvent.builder().failureMessage(failureMessage).build());
    }
}
