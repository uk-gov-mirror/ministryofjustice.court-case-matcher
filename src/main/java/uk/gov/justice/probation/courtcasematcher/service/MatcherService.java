package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;

import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatcherService {

    private final CourtCaseRestClient restClient;

    private final OffenderSearchRestClient offenderSearchRestClient;

    private final CaseMapper caseMapper;

    public void match(Case incomingCase) {
        Optional<CourtCase> existingCase = restClient.getCourtCase(incomingCase.getBlock().getSession().getCourtCode(), incomingCase.getCaseNo())
                .blockOptional();

        CourtCase courtCase = existingCase
                .map(existing -> caseMapper.merge(incomingCase, existing))
                .orElseGet(() -> newMatchedCaseOf(incomingCase)
                .orElseGet(() -> caseMapper.newFromCase(incomingCase)));

        restClient.putCourtCase(courtCase.getCourtCode(), courtCase.getCaseNo(), courtCase);
    }

    private Optional<CourtCase> newMatchedCaseOf(Case incomingCase) {
        return offenderSearchRestClient.search(incomingCase.getDef_name(), incomingCase.getDef_dob())
                .blockOptional()
                .map(searchResponse -> {
                    log.info(String.format("Match results for caseNo: %s, courtCode: %s - matchedBy: %s, matchCount: %s", incomingCase.getCaseNo(), incomingCase.getBlock().getSession().getCourtCode(), searchResponse.getMatchedBy(), searchResponse.getMatches().size()));
                    return searchResponse;
                })
                .filter(searchResponse -> searchResponse.getMatchedBy() == MatchType.ALL_SUPPLIED)
                .map(SearchResponse::getMatches)
                .flatMap(matches -> {
                    if (matches.size() == 1)
                        return Optional.ofNullable(matches.get(0));
                    else
                        return Optional.empty();
                })
                .map( match -> caseMapper.newFromCaseAndOffender(incomingCase, match.getOffender()))
                .or(() -> {
                    log.error(String.format("Match results for caseNo: %s, courtCode: %s - Empty response from OffenderSearchRestClient", incomingCase.getCaseNo(), incomingCase.getBlock().getSession().getCourtCode()));
                    return Optional.empty();
                });
    }
}
