package uk.gov.justice.probation.courtcasematcher.messaging;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.probation.courtcasematcher.messaging.MessageProcessorUtils.getCases;
import static uk.gov.justice.probation.courtcasematcher.messaging.MessageProcessorUtils.getCourtCodes;
import static uk.gov.justice.probation.courtcasematcher.messaging.MessageProcessorUtils.getHearingDates;

class MessageProcessorUtilsTest {

    private static final int DAY_OFFSET = 3;

    @DisplayName("Gets distinct set of hearing dates sorted")
    @Test
    void whenSessionsProvidedForBothDatesThenReturn() {

        LocalDate now = LocalDate.now();
        Session sessionToday1 = Session.builder().courtCode("SHF").dateOfHearing(now).courtRoom("1").build();
        Session sessionToday2 = Session.builder().courtCode("SHF").dateOfHearing(now).courtRoom("2").build();
        Session sessionFuture1 = Session.builder().courtCode("SHF").dateOfHearing(now.plusDays(3)).courtRoom("1").build();
        Session sessionFuture2 = Session.builder().courtCode("SHF").dateOfHearing(now.plusDays(3)).courtRoom("2").build();

        TreeSet<LocalDate> hearingDates = getHearingDates(Arrays.asList(sessionFuture1, sessionToday2, sessionFuture2, sessionToday1), DAY_OFFSET);
        assertThat(hearingDates).hasSize(2);
        assertThat(hearingDates.first()).isEqualTo(now);
        assertThat(hearingDates.last()).isEqualTo(now.plusDays(DAY_OFFSET));
    }

    @DisplayName("Gets distinct set of hearing dates sorted, despite only having input for today")
    @Test
    void whenSessionsProvidedForTodayOnly_ThenReturnTodayAndDerived() {

        LocalDate now = LocalDate.now();
        Session sessionToday1 = Session.builder().courtCode("SHF").dateOfHearing(now).courtRoom("1").build();
        Session sessionToday2 = Session.builder().courtCode("SHF").dateOfHearing(now).courtRoom("2").build();

        TreeSet<LocalDate> hearingDates = getHearingDates(Arrays.asList(sessionToday2, sessionToday1), DAY_OFFSET);
        assertThat(hearingDates).hasSize(2);
        assertThat(hearingDates.first()).isEqualTo(now);
        assertThat(hearingDates.last()).isEqualTo(now.plusDays(DAY_OFFSET));
    }

    @DisplayName("Gets distinct set of hearing dates sorted, despite only having input for the future date")
    @Test
    void whenSessionsProvidedForFutureOnly_ThenReturnTodayAndDerived() {

        LocalDate future = LocalDate.now().plusDays(DAY_OFFSET);
        Session session1 = Session.builder().courtCode("SHF").dateOfHearing(future).courtRoom("1").build();
        Session session2 = Session.builder().courtCode("SHF").dateOfHearing(future).courtRoom("2").build();

        TreeSet<LocalDate> hearingDates = getHearingDates(Arrays.asList(session2, session1), DAY_OFFSET);
        assertThat(hearingDates).hasSize(2);
        assertThat(hearingDates.first()).isEqualTo(LocalDate.now());
        assertThat(hearingDates.last()).isEqualTo(future);
    }

    @DisplayName("Although unexpected, three distinct dates will still work.")
    @Test
    void whenSessionsProvidedForThreeDates_ThenReturnAll() {

        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(DAY_OFFSET);
        Session sessionToday = Session.builder().courtCode("SHF").dateOfHearing(today).courtRoom("1").build();
        Session sessionFuture1 = Session.builder().courtCode("SHF").dateOfHearing(future).courtRoom("2").build();
        Session sessionFuture2 = Session.builder().courtCode("SHF").dateOfHearing(future.plusDays(1)).courtRoom("2").build();

        TreeSet<LocalDate> hearingDates = getHearingDates(Arrays.asList(sessionToday, sessionFuture1, sessionFuture2), DAY_OFFSET);
        assertThat(hearingDates).hasSize(3);
        assertThat(hearingDates.first()).isEqualTo(LocalDate.now());
        assertThat(hearingDates.last()).isEqualTo(future.plusDays(1));
    }

    @DisplayName("Gets list of the cases for a court")
    @Test
    void whenGetCasesForACourt() {
        Session session1 = Session.builder().courtCode("SHF")
            .blocks(Arrays.asList(
                Block.builder().cases(Arrays.asList(buildCase("100000001"), buildCase("100000002"))).build(),
                Block.builder().cases(Arrays.asList(buildCase("100000003"), buildCase("100000004"))).build()
            )).build();
        Session session2 = Session.builder().courtCode("BEV")
            .blocks(singletonList(
                Block.builder().cases(Arrays.asList(buildCase("10000005"), buildCase("100000006"))).build()
            )).build();

        List<Case> cases = getCases("SHF", Arrays.asList(session1, session2));

        assertThat(cases).hasSize(4);
        assertThat(cases).extracting("caseNo").contains("100000001", "100000002", "100000003", "100000004");
    }

    @DisplayName("Gets list of the cases for a court")
    @Test
    void whenGetCourtCodesFromSessions() {
        Session session1 = Session.builder()
            .courtCode("SHF")
            .courtRoom("1")
            .build();
        Session session2 = Session.builder()
            .courtCode("SHF")
            .courtRoom("1")
            .build();
        Session session3 = Session.builder()
            .courtCode("BEV")
            .build();

        Set<String> courtCodes = getCourtCodes(Arrays.asList(session1, session2, session3));

        assertThat(courtCodes).hasSize(2);
        assertThat(courtCodes).contains("SHF", "BEV");
    }

    private Case buildCase(String caseNo) {
        return Case.builder().caseNo(caseNo).build();
    }

}
