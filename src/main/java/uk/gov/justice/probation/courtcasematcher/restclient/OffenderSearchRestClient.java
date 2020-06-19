package uk.gov.justice.probation.courtcasematcher.restclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.event.OffenderSearchFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@SuppressWarnings("UnstableApiUsage")
@Component
@Slf4j
public class OffenderSearchRestClient {
    @Value("${offender-search.post-match-url}")
    private String postMatchUrl;

    private final WebClient webClient;

    private final EventBus eventBus;

    private final ObjectMapper objectMapper;

    @Autowired
    public OffenderSearchRestClient(@Qualifier("offender-search") WebClient webClient, EventBus eventBus, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        super();
        this.webClient = webClient;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
    }

    public Mono<SearchResponse> search(@NonNull String fullName, @NonNull LocalDate dateOfBirth){
        MatchRequest body = buildRequestBody(fullName, dateOfBirth);

        return webClient
                .post()
                .uri(postMatchUrl)
                .attributes(clientRegistrationId("offender-search-client"))
                .body(BodyInserters.fromPublisher(Mono.just(body), MatchRequest.class))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(SearchResponse.class)
                .onErrorResume(throwable -> handleError(throwable, body));
    }

    private Mono<? extends SearchResponse> handleError(Throwable throwable, MatchRequest body) {
        //  TODO: test this
        log.error(throwable.getMessage());
        String incomingMessage = null;
        try {
            // Would rather this didn't block but also want to provide a helpful error message
            // TODO: come up with a better solution
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

    private MatchRequest buildRequestBody(String fullName, LocalDate dateOfBirth) {
        return MatchRequest.builder()
                .firstName(NameHelper.getFirstName(fullName).toLowerCase())
                .surname(NameHelper.getSurname(fullName).toLowerCase())
                .dateOfBirth(dateOfBirth.format(DateTimeFormatter.ISO_DATE))
                .build();
    }

}
