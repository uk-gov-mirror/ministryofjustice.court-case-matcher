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
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchRequest;
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

    @Autowired
    private final MatchRequest.Factory matchRequestFactory;

    @Value("${probation-status-reference.default}")
    private final String defaultProbationStatus;

    @Value("${probation-status-reference.nonExactMatch}")
    private final String nonExactProbationStatus;

    public Mono<SearchResult> getSearchResponse(CourtCase courtCase) {
        final MatchRequest matchRequest;
        try {
            matchRequest = matchRequestFactory.buildFrom(courtCase);
        } catch (Exception e) {
            log.warn(String.format("Unable to create MatchRequest for caseNo: %s, courtCode: %s", courtCase.getCaseNo(), courtCase.getCourtCode()), e);
            throw e;
        }
        return Mono.defer(() -> Mono.just(matchRequest))
                .flatMap(offenderSearchRestClient::search)
                .map(searchResponse -> {
                    log.info(String.format("Match results for caseNo: %s, courtCode: %s - matchedBy: %s, matchCount: %s",
                            courtCase.getCaseNo(), courtCase.getCourtCode(), searchResponse.getMatchedBy(), searchResponse.getMatches() == null ? "null" : searchResponse.getMatches().size()));
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
                                searchResponse.getMatchCount(), courtCase.getDefendantName(), courtCase.getDefendantDob(), searchResponse.getMatchedBy());
                        String probationStatus = searchResponse.getMatchCount() >= 1 ? nonExactProbationStatus : defaultProbationStatus;
                        return Mono.zip(Mono.just(searchResponse), Mono.just(probationStatus));
                    }
                })
                .map(this::combine)
                .doOnSuccess((data) -> {
                    if (data == null) {
                        log.error(String.format("Match results for caseNo: %s, courtCode: %s - Empty response from OffenderSearchRestClient",
                                courtCase.getCaseNo(), courtCase.getCourtCode()));
                    }
                })
                .map(searchResponse -> SearchResult.builder()
                        .searchResponse(searchResponse)
                        .matchRequest(matchRequest)
                        .build());
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
