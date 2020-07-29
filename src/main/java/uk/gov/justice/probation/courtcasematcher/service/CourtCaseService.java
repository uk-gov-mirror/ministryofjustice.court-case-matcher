package uk.gov.justice.probation.courtcasematcher.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
@AllArgsConstructor
public class CourtCaseService {

    @Autowired
    private final CourtCaseRestClient restClient;

    public void purgeAbsent(String courtCode, Set<LocalDate> hearingDates, List<Case> cases) {

        Map<LocalDate, List<String>> casesByDate = cases.stream()
            .sorted(Comparator.comparing(Case::getCaseNo))
            .collect(groupingBy(aCase -> aCase.getBlock().getSession().getDateOfHearing(), TreeMap::new, mapping(Case::getCaseNo, toList())));

        hearingDates.forEach(hearingDate -> casesByDate.putIfAbsent(hearingDate, Collections.emptyList()));

        restClient.purgeAbsent(courtCode, casesByDate);
    }

}
