package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.model.mapper.MatchDetails;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;

import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class CourtCaseService {

    @Autowired
    private final CourtCaseRestClient restClient;

    public Mono<CourtCase> getCourtCase(Case aCase) {
        return restClient.getCourtCase(aCase.getBlock().getSession().getCourtCode(), aCase.getCaseNo())
            .map(existing -> CaseMapper.merge(aCase, existing))
            .switchIfEmpty(Mono.defer(() -> Mono.just(CaseMapper.newFromCase(aCase))));
    }

    public void createCase(CourtCase courtCase, SearchResult searchResult) {

        Optional.ofNullable(searchResult)
            .ifPresentOrElse(result -> {
                    var response = result.getSearchResponse();
                    log.debug("Save court case with search response for case {}, court {}",
                        courtCase.getCaseNo(), courtCase.getCourtCode());
                    saveCourtCase(CaseMapper.newFromCourtCaseWithMatches(courtCase, MatchDetails.builder()
                            .matchType(MatchType.of(result))
                            .matches(response.getMatches())
                            .exactMatch(response.isExactMatch())
                            .build()));
                },
                () -> saveCourtCase(courtCase));
    }

    public void saveCourtCase(CourtCase courtCase) {
        restClient.putCourtCase(courtCase.getCourtCode(), courtCase.getCaseNo(), courtCase);
    }

    public Mono<CourtCase> updateProbationStatusDetail(CourtCase courtCase) {
        return restClient.getProbationStatusDetail(courtCase.getCrn())
            .map(probationStatusDetail -> CaseMapper.merge(probationStatusDetail, courtCase))
            .switchIfEmpty(Mono.just(courtCase));
    }

}
