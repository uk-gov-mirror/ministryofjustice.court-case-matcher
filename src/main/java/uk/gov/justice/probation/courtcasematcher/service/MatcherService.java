package uk.gov.justice.probation.courtcasematcher.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;

@Service
@Slf4j
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatcherService {

    @Autowired
    private final CourtCaseRestClient restClient;

    @Autowired
    private final OffenderSearchRestClient offenderSearchRestClient;

    @Value("${probation-status-reference.default}")
    private final String defaultProbationStatus;

    @Value("${probation-status-reference.nonExactMatch}")
    private final String nonExactProbationStatus;

    public Mono<SearchResponse> getSearchResponse(String defendantName, LocalDate dateOfBirth, String courtCode, String caseNo) {
        return offenderSearchRestClient.search(defendantName, dateOfBirth)
                .map(searchResponse -> {
                    log.info(String.format("Match results for caseNo: %s, courtCode: %s - matchedBy: %s, matchCount: %s",
                        caseNo, courtCode, searchResponse.getMatchedBy(), searchResponse.getMatches() == null ? "null" : searchResponse.getMatches().size()));
                    return searchResponse;
                })
                .flatMap(searchResponse -> {
                    List<Match> matches = searchResponse.getMatches();
                    if (searchResponse.isExactMatch()) {
                        return Mono.zip(Mono.just(searchResponse), restClient.getProbationStatus(matches.get(0).getOffender().getOtherIds().getCrn()));
                    }
                    else {
                        int matchCount = Optional.ofNullable(matches).map(List::size).orElse(0);
                        log.debug("Got {} matches for defendant name {}, dob {}, match type {}",
                            matchCount, defendantName, dateOfBirth, searchResponse.getMatchedBy());
                        String probationStatus = matchCount >= 1 ? nonExactProbationStatus : defaultProbationStatus;
                        return Mono.zip(Mono.just(searchResponse), Mono.just(probationStatus));
                    }
                })
                .map(this::combine)
                .doOnSuccess((data) -> {
                    if (data == null) {
                        log.error(String.format("Match results for caseNo: %s, courtCode: %s - Empty response from OffenderSearchRestClient",
                            caseNo, courtCode));
                    }
                });
    }

    private SearchResponse combine(Tuple2<SearchResponse, String> tuple2) {
        SearchResponse searchResponse = tuple2.getT1();
        String probationStatus = tuple2.getT2();
        return SearchResponse.builder().matchedBy(tuple2.getT1().getMatchedBy())
            .probationStatus(StringUtils.isEmpty(probationStatus) ? null : probationStatus)
            .matches(searchResponse.getMatches())
            .build();
    }

}
