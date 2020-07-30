package uk.gov.justice.probation.courtcasematcher.messaging;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Document;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.InfoSourceDetail;
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

    /**
     * Workaround for tactical solution. We receive a document each time a court list is printed for a date.  There should only be one for
     * a court code / date of hearing. We can find the correct one by looking for the one with the highest sequence number. This sequence
     * number is found within a string which is "source_file_name".
     *
     * @param sourceDocuments
     *          the source documents sent in the payload
     * @return
     *          a de-duplicated list of documents
     */
    static List<Document> getUniqueDocuments(List<Document> sourceDocuments) {
        Set<Info> uniqueInfo = sourceDocuments.stream().map(Document::getInfo).collect(Collectors.toSet());
        if (sourceDocuments.size() == uniqueInfo.size()) {
            log.info("No duplicates found for input documents, size {}", sourceDocuments.size());
            return sourceDocuments;
        }

        log.debug("Got {} unique info from {} source documents", uniqueInfo.size(), sourceDocuments.size());

        List<Document> result = new ArrayList<>(uniqueInfo.size());
        uniqueInfo.forEach(info -> {
            long maxSequence = getMaxSequenceNumber(sourceDocuments, info);
            log.debug("Max for court {} on {} is {}", info.getCourtHouse(), info.getDateOfHearing(), maxSequence);

            sourceDocuments.stream()
                .filter(document -> document.getInfo().equals(info) && maxSequence == document.getInfo().getInfoSourceDetail().getSequence())
                .findFirst()
                .ifPresentOrElse(result::add,() -> log.error("Could not match for info {} and sequence value {}", info, maxSequence));
        });

        return result;
    }

    static long getMaxSequenceNumber(List<Document> documents, Info info) {
        InfoSourceDetail infoSourceDetail = documents.stream()
            .map(Document::getInfo)
            .filter(info::equals)
            .map(Info::getInfoSourceDetail)
            .max(Comparator.comparing(InfoSourceDetail::getSequence))
            .orElse(null);

        return infoSourceDetail != null ? infoSourceDetail.getSequence() : 0;
    }

}
