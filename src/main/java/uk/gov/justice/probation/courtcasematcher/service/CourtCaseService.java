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

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
@AllArgsConstructor
public class CourtCaseService {

    @Autowired
    private final CourtCaseRestClient restClient;

    @Autowired
    private final CaseMapper caseMapper;

    public Mono<CourtCase> getCourtCase(Case aCase) {
        return restClient.getCourtCase(aCase.getBlock().getSession().getCourtCode(), aCase.getCaseNo())
            .map(existing -> caseMapper.merge(aCase, existing))
            .switchIfEmpty(Mono.defer(() -> Mono.just(caseMapper.newFromCase(aCase))));
    }

    public void createCase(CourtCase courtCase, SearchResult searchResult) {

        Optional.ofNullable(searchResult)
            .ifPresentOrElse(result -> {
                    var response = result.getSearchResponse();
                    log.debug("Save court case with search response for case {}, court {}",
                        courtCase.getCaseNo(), courtCase.getCourtCode());
                    saveCourtCase(caseMapper.newFromCourtCaseWithMatches(courtCase, MatchDetails.builder()
                            .matchType(MatchType.of(result))
                            .matches(response.getMatches())
                            .probationStatusDetail(response.getProbationStatusDetail())
                            .exactMatch(response.isExactMatch())
                            .build()));
                },
                () -> saveCourtCase(courtCase));
    }

    public void saveCourtCase(CourtCase courtCase) {
        restClient.putCourtCase(courtCase.getCourtCode(), courtCase.getCaseNo(), courtCase);
    }

    public void purgeAbsent(String courtCode, Set<LocalDate> hearingDates, List<Case> cases) {

        Map<LocalDate, List<String>> casesByDate = cases.stream()
            .sorted(Comparator.comparing(Case::getCaseNo))
            .collect(groupingBy(aCase -> aCase.getBlock().getSession().getDateOfHearing(), TreeMap::new, mapping(Case::getCaseNo, toList())));

        hearingDates.forEach(hearingDate -> casesByDate.putIfAbsent(hearingDate, Collections.emptyList()));

        restClient.purgeAbsent(courtCode, casesByDate);
    }

}
