package uk.gov.justice.probation.courtcasematcher.service;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourtCaseServiceTest {

    private static final LocalDate JAN_1 = LocalDate.of(2020, Month.JANUARY, 1);
    private static final LocalDate JAN_3 = LocalDate.of(2020, Month.JANUARY, 3);

    @Mock
    private CourtCaseRestClient restClient;

    @InjectMocks
    private CourtCaseService courtCaseService;

    @DisplayName("Purge with 5 cases across 4 sessions and 2 days")
    @Test
    void purgeAbsent() {

        Session sessionJan1a = Session.builder().dateOfHearing(JAN_1).courtCode("SHF").build();
        Session sessionJan1b = Session.builder().dateOfHearing(JAN_1).courtCode("SHF").build();

        Session sessionJan3a = Session.builder().dateOfHearing(JAN_3).courtCode("SHF").build();
        Session sessionJan3b = Session.builder().dateOfHearing(JAN_3).courtCode("SHF").build();

        Case aCase10 = Case.builder().caseNo("1010").block(Block.builder().session(sessionJan1a).build()).build();
        Case aCase20 = Case.builder().caseNo("1011").block(Block.builder().session(sessionJan1b).build()).build();
        Case aCase21 = Case.builder().caseNo("1030").block(Block.builder().session(sessionJan3a).build()).build();
        Case aCase30 = Case.builder().caseNo("1031").block(Block.builder().session(sessionJan3a).build()).build();
        Case aCase31 = Case.builder().caseNo("1032").block(Block.builder().session(sessionJan3b).build()).build();

        List<Case> allCases = asList(aCase20, aCase21, aCase10, aCase30, aCase31);

        courtCaseService.purgeAbsent("SHF", Set.of(JAN_1, JAN_3), allCases);

        Map<LocalDate, List<String>> expected = Map.of(JAN_1, asList("1010", "1011"), JAN_3, asList("1030", "1031", "1032"));
        verify(restClient).purgeAbsent("SHF", expected);
    }

    @DisplayName("Purge with 2 cases across 1 sessions and 2 days. One day has no cases.")
    @Test
    void purgeAbsentNoCases() {


        Session sessionJan3 = Session.builder().dateOfHearing(JAN_3).courtCode("SHF").build();

        Case aCase30 = Case.builder().caseNo("1031").block(Block.builder().session(sessionJan3).build()).build();
        Case aCase31 = Case.builder().caseNo("1032").block(Block.builder().session(sessionJan3).build()).build();

        courtCaseService.purgeAbsent("SHF", Set.of(JAN_1, JAN_3), asList(aCase30, aCase31));

        Map<LocalDate, List<String>> expected = Map.of(JAN_1, Collections.emptyList(), JAN_3, asList("1031", "1032"));
        verify(restClient).purgeAbsent("SHF", expected);
    }
}
