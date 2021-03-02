package uk.gov.justice.probation.courtcasematcher.service;

import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.ProbationStatus;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;

@Service
@Slf4j
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatcherService {

    @Autowired
    private final FeatureFlags featureFlags;

    @Autowired
    private final CourtCaseRestClient restClient;

    @Autowired
    private final OffenderSearchRestClient offenderSearchRestClient;

    @Autowired
    private final MatchRequest.Factory matchRequestFactory;

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
                        return Mono.zip(Mono.just(searchResponse), getProbationStatus(searchResponse.getMatches().get(0).getOffender()));
                    }
                    else {
                        log.debug("Got {} matches for defendant name {}, dob {}, match type {}",
                                searchResponse.getMatchCount(), courtCase.getDefendantName(), courtCase.getDefendantDob(), searchResponse.getMatchedBy());
                        return Mono.zip(Mono.just(searchResponse), Mono.just(ProbationStatusDetail.builder().build()));
                    }
                })
                .map(this::combine)
                .doOnSuccess((data) -> {
                    if (data == null) {
                        log.info(String.format("Match results for caseNo: %s, courtCode: %s - Empty response from OffenderSearchRestClient",
                                courtCase.getCaseNo(), courtCase.getCourtCode()));
                    }
                })
                .map(searchResponse -> SearchResult.builder()
                        .searchResponse(searchResponse)
                        .matchRequest(matchRequest)
                        .build());
    }

    // TODO - when offender search gives us the probation status we can remove this method
    @Deprecated(forRemoval = true)
    private Mono<ProbationStatusDetail> getProbationStatus(Offender offender) {
        if (featureFlags.offenderSearchForProbationStatus()) {
            var probationStatus = offender.getProbationStatus();
            return Mono.just(ProbationStatusDetail.builder()
                .probationStatus(Optional.ofNullable(probationStatus).map(ProbationStatus::getStatus).orElse(null))
                .inBreach(Optional.ofNullable(probationStatus).map(ProbationStatus::getInBreach).orElse(null))
                .previouslyKnownTerminationDate(Optional.ofNullable(probationStatus).map(ProbationStatus::getPreviouslyKnownTerminationDate).orElse(null))
                .preSentenceActivity(Optional.ofNullable(probationStatus).map(ProbationStatus::isPreSentenceActivity).orElse(false))
                .build());
        }
        return restClient.getProbationStatus(offender.getOtherIds().getCrn());
    }

    // TODO - when offender search gives us the probation status we can remove this method
    @Deprecated(forRemoval = true)
    private SearchResponse combine(Tuple2<SearchResponse, ProbationStatusDetail> tuple2) {
        SearchResponse searchResponse = tuple2.getT1();
        ProbationStatusDetail probationStatus = tuple2.getT2();

        if (searchResponse.isExactMatch() && !featureFlags.offenderSearchForProbationStatus()) {
            var offender = searchResponse.getMatches().get(0).getOffender();
            var match = Match.builder()
                .offender(Offender.builder()
                        .otherIds(offender.getOtherIds())
                        .probationStatus(ProbationStatus.builder()
                                        .inBreach(probationStatus.getInBreach())
                                        .preSentenceActivity(probationStatus.isPreSentenceActivity())
                                        .previouslyKnownTerminationDate(probationStatus.getPreviouslyKnownTerminationDate())
                                        .status(probationStatus.getProbationStatus())
                                        .build())
                        .build())
                .build();
            return SearchResponse.builder()
                .matchedBy(searchResponse.getMatchedBy())
                .matches(List.of(match))
                .build();
        }
        return searchResponse;
    }

}
