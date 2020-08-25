package uk.gov.justice.probation.courtcasematcher.messaging;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;

@Slf4j
public final class MessageProcessorUtils {


    static Set<String> getCourtCodes(List<Session> sessions) {
        return sessions.stream()
            .map(Session::getCourtCode)
            .collect(Collectors.toSet());
    }

    static List<Case> getCases(String courtCode, List<Session> sessions) {
        return sessions.stream()
            .filter(session -> session.getCourtCode().equals(courtCode))
            .flatMap(session -> session.getBlocks().stream())
            .flatMap(block -> block.getCases().stream())
            .collect(Collectors.toList());
    }

    static TreeSet<LocalDate> getHearingDates(List<Session> sessions, int caseFeedFutureDateOffset) {

        TreeSet<LocalDate> hearingDates = sessions.stream().map(Session::getDateOfHearing).collect(Collectors.toCollection(TreeSet::new));
        // This has to be here because instead of sending us a date / court with no cases, if there are no cases on a date, we need to purge
        // for that date
        int hearingDatesSize = hearingDates.size();
        if (hearingDatesSize > 2 ) {
            log.warn("More than expected count of distinct hearing dates. Was {}, expected 2", hearingDatesSize);
        }
        if (hearingDatesSize == 1){
            LocalDate today = LocalDate.now();
            LocalDate hearingDate = hearingDates.first();
            if (hearingDate.equals(LocalDate.now())) {
                hearingDates.add(LocalDate.now().plusDays(caseFeedFutureDateOffset));
            }
            else if (hearingDate.isAfter(today)) {
                hearingDates.add(hearingDate.minusDays(caseFeedFutureDateOffset));
            }
        }
        return hearingDates;
    }

}
