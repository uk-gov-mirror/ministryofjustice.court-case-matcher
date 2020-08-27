package uk.gov.justice.probation.courtcasematcher.service;

import static uk.gov.justice.probation.courtcasematcher.model.offendersearch.OffenderSearchMatchType.ALL_SUPPLIED;

import java.util.Collections;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;
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
    private final CaseMapper caseMapper;

    public void match(Case incomingCase) {
        restClient.getCourtCase(incomingCase.getBlock().getSession().getCourtCode(), incomingCase.getCaseNo())
                .map(existing -> caseMapper.merge(incomingCase, existing))
                .switchIfEmpty(Mono.defer(() -> newMatchedCaseOf(incomingCase)))
                .switchIfEmpty(Mono.defer(() -> Mono.just(caseMapper.newFromCase(incomingCase))))
                .map(courtCase -> restClient.putCourtCase(courtCase.getCourtCode(), courtCase.getCaseNo(), courtCase))
                .block();
    }

    private Mono<CourtCase> newMatchedCaseOf(Case incomingCase) {
        final String courtCode = incomingCase.getBlock().getSession().getCourtCode();
        final String caseNo = incomingCase.getCaseNo();
        return offenderSearchRestClient.search(incomingCase.getDef_name(), incomingCase.getDef_dob())
                .map(searchResponse -> {
                    log.info(String.format("Match results for caseNo: %s, courtCode: %s - matchedBy: %s, matchCount: %s",
                        caseNo, courtCode, searchResponse.getMatchedBy(), searchResponse.getMatches() == null ? "null" : searchResponse.getMatches().size()));
                    return searchResponse;
                })
                .filter(searchResponse -> searchResponse.getMatchedBy() == ALL_SUPPLIED)
                .map(SearchResponse::getMatches)
                .flatMap(matches -> {
                    if (matches != null && matches.size() == 1)
                        return Mono.zip(Mono.just(matches.get(0)), restClient.getOffenderProbationStatus(matches.get(0).getOffender().getOtherIds().getCrn()));
                    else
                        return Mono.empty();
                })
                .map(tuple2 -> buildNewCase(tuple2, incomingCase))
                // Ideally we would avoid blocking at this point and continue with Mono processing
                .doOnSuccess((data) -> {
                    if (data == null) {
                        log.error(String.format("Match results for caseNo: %s, courtCode: %s - Empty response from OffenderSearchRestClient",
                            incomingCase.getCaseNo(), incomingCase.getBlock().getSession().getCourtCode()));
                    }
                });
    }

    private CourtCase buildNewCase(Tuple2<Match, String> tuple, Case incomingCase) {
        Match match = tuple.getT1();
        String probationStatus = tuple.getT2();
        return caseMapper.newFromCaseAndOffender(incomingCase, match.getOffender(), probationStatus, buildGroupedOffenderMatch(match, MatchType.of(ALL_SUPPLIED)));
    }

    private GroupedOffenderMatches buildGroupedOffenderMatch (Match offenderMatch, MatchType matchType) {

        return GroupedOffenderMatches.builder()
            .matches(Collections.singletonList(OffenderMatch.builder()
                        .confirmed(false)
                        .matchType(matchType)
                        .matchIdentifiers(MatchIdentifiers.builder()
                            .pnc(offenderMatch.getOffender().getOtherIds().getPnc())
                            .cro(offenderMatch.getOffender().getOtherIds().getCro())
                            .crn(offenderMatch.getOffender().getOtherIds().getCrn())
                            .build())
                        .build()))
            .build();
    }

}
