package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;

import java.util.Optional;

@Service
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
                .map(SearchResponse::getMatches)
                .flatMap(matches -> {
                    if (matches.size() == 1)
                        return Optional.ofNullable(matches.get(0));
                    else
                        return Optional.empty();
                })
                .map( match -> caseMapper.newFromCaseAndOffender(incomingCase, match.getOffender()));
    }
}
