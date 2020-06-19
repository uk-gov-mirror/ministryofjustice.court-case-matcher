package uk.gov.justice.probation.courtcasematcher.restclient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class OffenderSearchRestClient {
    @Value("${offender-search.post-match-url}")
    private String postMatchUrl;

    private final WebClient webClient;

    @Autowired
    public OffenderSearchRestClient(@Qualifier("offender-search") WebClient webClient) {
        super();
        this.webClient = webClient;
    }

    public Mono<SearchResponse> search(String fullName, LocalDate dateOfBirth){

        return webClient
                .post()
                .uri(postMatchUrl)
                .body(BodyInserters.fromPublisher(buildRequestBody(fullName, dateOfBirth), MatchRequest.class))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(SearchResponse.class);
    }

    private Mono<MatchRequest> buildRequestBody(String fullName, LocalDate dateOfBirth) {
        return Mono.just(MatchRequest.builder()
                .firstName(NameHelper.getFirstName(fullName).toLowerCase())
                .surname(NameHelper.getSurname(fullName).toLowerCase())
                .dateOfBirth(dateOfBirth.format(DateTimeFormatter.ISO_DATE))
                .build());
    }
}
