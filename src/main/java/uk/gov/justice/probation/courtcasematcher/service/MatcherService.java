package uk.gov.justice.probation.courtcasematcher.service;

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
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;

import java.time.LocalDate;

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

    public Mono<SearchResponse> getSearchResponse(Name defendantName, LocalDate dateOfBirth, String courtCode, String caseNo) {
        return offenderSearchRestClient.search(defendantName, dateOfBirth)
                .map(searchResponse -> {
                    log.info(String.format("Match results for caseNo: %s, courtCode: %s - matchedBy: %s, matchCount: %s",
                        caseNo, courtCode, searchResponse.getMatchedBy(), searchResponse.getMatches() == null ? "null" : searchResponse.getMatches().size()));
                    return searchResponse;
                })
                .flatMap(searchResponse -> {
                    if (searchResponse.isExactMatch()) {
                        return Mono.zip(Mono.just(searchResponse), restClient.getProbationStatus(searchResponse.getMatches()
                                                                                                    .get(0)
                                                                                                    .getOffender()
                                                                                                    .getOtherIds()
                                                                                                    .getCrn()));
                    }
                    else {
                        log.debug("Got {} matches for defendant name {}, dob {}, match type {}",
                                searchResponse.getMatchCount(), defendantName, dateOfBirth, searchResponse.getMatchedBy());
                        String probationStatus = searchResponse.getMatchCount() >= 1 ? nonExactProbationStatus : defaultProbationStatus;
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
