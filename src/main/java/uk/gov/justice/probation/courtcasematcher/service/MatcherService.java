package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
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
        return offenderSearchRestClient.search(incomingCase.getDef_name(), incomingCase.getDef_dob())
                .map(searchResponse -> {
                    log.info(String.format("Match results for caseNo: %s, courtCode: %s - matchedBy: %s, matchCount: %s", incomingCase.getCaseNo(), incomingCase.getBlock().getSession().getCourtCode(), searchResponse.getMatchedBy(), searchResponse.getMatches() == null ? "null" : searchResponse.getMatches().size()));
                    return searchResponse;
                })
                .filter(searchResponse -> searchResponse.getMatchedBy() == MatchType.ALL_SUPPLIED)
                .map(SearchResponse::getMatches)
                .flatMap(matches -> {
                    if (matches != null && matches.size() == 1)
                        return Mono.just(matches.get(0));
                    else
                        return Mono.empty();
                })
                .map( match -> caseMapper.newFromCaseAndOffender(incomingCase, match.getOffender()))
                // Ideally we would avoid blocking at this point and continue with Mono processing
                .doOnSuccess((data) -> {
                    if (data == null) {
                        log.error(String.format("Match results for caseNo: %s, courtCode: %s - Empty response from OffenderSearchRestClient", incomingCase.getCaseNo(), incomingCase.getBlock().getSession().getCourtCode()));
                    }
                });
    }
}
