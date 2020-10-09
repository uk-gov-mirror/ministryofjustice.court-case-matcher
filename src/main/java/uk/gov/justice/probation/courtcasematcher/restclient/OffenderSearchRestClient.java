package uk.gov.justice.probation.courtcasematcher.restclient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.StringUtils;
import uk.gov.justice.probation.courtcasematcher.event.OffenderSearchFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@SuppressWarnings("UnstableApiUsage")
@Component
@Slf4j
public class OffenderSearchRestClient {
    private static final String ERROR_NO_DATE_OF_BIRTH = "No dateOfBirth provided";
    private static final String ERROR_NO_NAME = "No surname provided";
    @Value("${offender-search.post-match-url}")
    private String postMatchUrl;

    @Value("${offender-search.disable-authentication:false}")
    private Boolean disableAuthentication;

    private final WebClient webClient;

    private final EventBus eventBus;

    private final ObjectMapper objectMapper;

    @Autowired
    public OffenderSearchRestClient(@Qualifier("offenderSearchWebClient") WebClient webClient, EventBus eventBus, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        super();
        this.webClient = webClient;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
    }

    public Mono<SearchResponse> search(Name name, LocalDate dateOfBirth){
        if (!validate(name, dateOfBirth)) {
            return Mono.empty();
        }
        MatchRequest body = buildRequestBody(name, dateOfBirth);
        return post()
                .body(BodyInserters.fromPublisher(Mono.just(body), MatchRequest.class))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(SearchResponse.class)
                .onErrorResume(throwable -> handleError(throwable, body));
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

    private Mono<? extends SearchResponse> handleError(Throwable throwable, MatchRequest body) {
        String incomingMessage = null;
        try {
            // Would rather this didn't block but also want to provide a helpful error message
            // TODO: come up with a better solution
            // We could send a toString representation of the messageBody or do the parsing to JSON in the EventBus
            //noinspection BlockingMethodInNonBlockingContext
            incomingMessage = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        eventBus.post(OffenderSearchFailureEvent.builder()
                .failureMessage(throwable.getMessage())
                .requestJson(incomingMessage)
                .build());
        return Mono.empty();
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

}
