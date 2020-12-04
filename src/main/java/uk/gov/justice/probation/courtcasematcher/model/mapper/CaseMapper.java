package uk.gov.justice.probation.courtcasematcher.model.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.Address;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase.CourtCaseBuilder;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.Offence;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Component
@Slf4j
public class CaseMapper {

    private final String defaultProbationStatus;

    public CaseMapper(@Value("${probation-status-reference.default}") String defaultProbationStatus) {
        super();
        this.defaultProbationStatus = defaultProbationStatus;
    }

    public CourtCase newFromCase(Case aCase) {
        return getCourtCaseBuilderFromCase(aCase)
            .isNew(true)
            .build();
    }

    private CourtCase.CourtCaseBuilder getCourtCaseBuilderFromCase(CourtCase courtCase, String probationStatus) {
        return CourtCase.builder()
            .caseNo(courtCase.getCaseNo())
            .courtCode(courtCase.getCourtCode())
            .caseId(String.valueOf(courtCase.getCaseId()))
            .courtRoom(courtCase.getCourtRoom())
            .defendantAddress(courtCase.getDefendantAddress())
            .defendantName(courtCase.getDefendantName())
            .name(courtCase.getName())
            .defendantDob(courtCase.getDefendantDob())
            .defendantSex(courtCase.getDefendantSex())
            .defendantType(courtCase.getDefendantType())
            .cro(courtCase.getCro())
            .pnc(courtCase.getPnc())
            .listNo(courtCase.getListNo())
            .sessionStartTime(courtCase.getSessionStartTime())
            .probationStatus(Optional.ofNullable(probationStatus).orElse(defaultProbationStatus))
            .nationality1(courtCase.getNationality1())
            .nationality2(courtCase.getNationality2())
            .offences(courtCase.getOffences());
    }

    private CourtCase.CourtCaseBuilder getCourtCaseBuilderFromCase(Case aCase) {
        return CourtCase.builder()
            .caseNo(aCase.getCaseNo())
            .courtCode(aCase.getBlock().getSession().getCourtCode())
            .caseId(String.valueOf(aCase.getId()))
            .courtRoom(aCase.getBlock().getSession().getCourtRoom())
            .defendantAddress(Optional.ofNullable(aCase.getDef_addr()).map(CaseMapper::fromAddress).orElse(null))
            .name(aCase.getName())
            .defendantName(nameFrom(aCase.getDef_name(), aCase.getName()))
            .defendantDob(aCase.getDef_dob())
            .defendantSex(aCase.getDef_sex())
            .defendantType(DefendantType.of(aCase.getDef_type()))
            .cro(aCase.getCro())
            .pnc(aCase.getPnc())
            .listNo(aCase.getListNo())
            .sessionStartTime(aCase.getBlock().getSession().getSessionStartTime())
            .probationStatus(defaultProbationStatus)
            .nationality1(aCase.getNationality1())
            .nationality2(aCase.getNationality2())
            .offences(Optional.ofNullable(aCase.getOffences()).map(CaseMapper::fromOffences).orElse(Collections.emptyList()));
    }

    private static List<Offence> fromOffences(List<uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence> offences) {
        return offences.stream()
            .sorted(comparing(uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence::getSeq))
            .map(offence -> Offence.builder()
                                .offenceTitle(offence.getTitle())
                                .offenceSummary(offence.getSum())
                                .sequenceNumber(offence.getSeq())
                                .act(offence.getAs())
                                .build())
            .collect(Collectors.toList());
    }

    public static Address fromAddress(uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Address def_addr) {
        return Address.builder()
            .line1(def_addr.getLine1())
            .line2(def_addr.getLine2())
            .line3(def_addr.getLine3())
            .line4(def_addr.getLine4())
            .line5(def_addr.getLine5())
            .postcode(def_addr.getPcode())
            .build();
    }

    public static String nameFrom(String defendantName, Name name) {
        return Optional.ofNullable(defendantName)
                        .orElse(Optional.ofNullable(name)
                                .map(Name::getFullName)
                                .orElse(null));
    }

    public CourtCase merge(Case incomingCase, CourtCase existingCourtCase) {
        return CourtCase.builder()
            // PK fields
            .courtCode(incomingCase.getBlock().getSession().getCourtCode())
            .caseNo(existingCourtCase.getCaseNo())
            // Fields to be updated from incoming
            .caseId(String.valueOf(incomingCase.getId()))
            .courtRoom(incomingCase.getBlock().getSession().getCourtRoom())
            .defendantAddress(fromAddress(incomingCase.getDef_addr()))
            .name(incomingCase.getName())
            .defendantName(nameFrom(incomingCase.getDef_name(), incomingCase.getName()))
            .defendantSex(incomingCase.getDef_sex())
            .defendantDob(incomingCase.getDef_dob())
            .defendantType(DefendantType.of(incomingCase.getDef_type()))
            .listNo(incomingCase.getListNo())
            .sessionStartTime(incomingCase.getBlock().getSession().getSessionStartTime())
            .offences(fromOffences(incomingCase.getOffences()))
            .nationality1(incomingCase.getNationality1())
            .nationality2(incomingCase.getNationality2())
            // Fields to be retained from existing court case
            .breach(existingCourtCase.getBreach())
            .previouslyKnownTerminationDate(existingCourtCase.getPreviouslyKnownTerminationDate())
            .crn(existingCourtCase.getCrn())
            .probationStatus(existingCourtCase.getProbationStatus())
            .suspendedSentenceOrder(existingCourtCase.getSuspendedSentenceOrder())
            .pnc(existingCourtCase.getPnc())

            .build();
    }

    public CourtCase newFromCourtCaseWithMatches(CourtCase incomingCase, MatchDetails matchDetails) {

        ProbationStatusDetail statusDetail = matchDetails.getProbationStatusDetail();

        CourtCaseBuilder courtCaseBuilder = getCourtCaseBuilderFromCase(incomingCase, Optional.ofNullable(statusDetail)
                                                                                        .map(ProbationStatusDetail::getProbationStatus)
                                                                                        .orElse(defaultProbationStatus))
                                            .groupedOffenderMatches(buildGroupedOffenderMatch(matchDetails.getMatches(), matchDetails.getMatchType()));

        if (matchDetails.isExactMatch()) {
            Match match = matchDetails.getMatches().get(0);

            courtCaseBuilder
                .breach(Optional.ofNullable(statusDetail).map(ProbationStatusDetail::getInBreach).orElse(null))
                .previouslyKnownTerminationDate(Optional.ofNullable(statusDetail).map(ProbationStatusDetail::getPreviouslyKnownTerminationDate).orElse(null))
                .crn(match.getOffender().getOtherIds().getCrn())
                .cro(match.getOffender().getOtherIds().getCro())
                .pnc(match.getOffender().getOtherIds().getPnc())
                .build();
        }

        return courtCaseBuilder.build();
    }

    private GroupedOffenderMatches buildGroupedOffenderMatch (List<Match> matches, MatchType matchType) {

        if (matches == null || matches.isEmpty()) {
            return GroupedOffenderMatches.builder().matches(Collections.emptyList()).build();
        }
        return GroupedOffenderMatches.builder()
            .matches(matches.stream()
                .map(match -> buildOffenderMatch(matchType, match))
                .collect(Collectors.toList()))
            .build();
    }

    private OffenderMatch buildOffenderMatch(MatchType matchType, Match match) {
        return OffenderMatch.builder()
            .rejected(false)
            .confirmed(false)
            .matchType(matchType)
            .matchIdentifiers(MatchIdentifiers.builder()
                .pnc(match.getOffender().getOtherIds().getPnc())
                .cro(match.getOffender().getOtherIds().getCro())
                .crn(match.getOffender().getOtherIds().getCrn())
                .build())
            .build();
    }
}
