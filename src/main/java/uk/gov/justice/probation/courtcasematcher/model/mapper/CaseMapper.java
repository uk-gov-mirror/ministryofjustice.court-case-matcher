package uk.gov.justice.probation.courtcasematcher.model.mapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@AllArgsConstructor
@Component
@Slf4j
public class CaseMapper {

    public static CourtCase newFromCase(Case aCase) {
        return newFromLibraCase(aCase)
            .isNew(true)
            .build();
    }

    private static CourtCase.CourtCaseBuilder newFromCourtCase(CourtCase courtCase) {
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
            .nationality1(courtCase.getNationality1())
            .nationality2(courtCase.getNationality2())
            .preSentenceActivity(courtCase.isPreSentenceActivity())
            .offences(courtCase.getOffences());
    }

    private static CourtCase.CourtCaseBuilder newFromLibraCase(Case aCase) {
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
            .nationality1(aCase.getNationality1())
            .nationality2(aCase.getNationality2())
            .offences(Optional.ofNullable(aCase.getOffences()).map(CaseMapper::fromOffences).orElse(Collections.emptyList()));
    }

    private static List<Offence> fromOffences(List<uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence> offences) {
        return Optional.ofNullable(offences)
                        .map(offs -> offs.stream()
                            .sorted(comparing(uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence::getSeq))
                            .map(offence -> Offence.builder()
                                .offenceTitle(offence.getTitle())
                                .offenceSummary(offence.getSum())
                                .sequenceNumber(offence.getSeq())
                                .act(offence.getAs())
                                .build())
                            .collect(Collectors.toList()))
                        .orElse(Collections.emptyList());
    }

    public static Address fromAddress(uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Address def_addr) {
        return Optional.ofNullable(def_addr)
                        .map(address -> Address.builder()
                                        .line1(def_addr.getLine1())
                                        .line2(def_addr.getLine2())
                                        .line3(def_addr.getLine3())
                                        .line4(def_addr.getLine4())
                                        .line5(def_addr.getLine5())
                                        .postcode(def_addr.getPcode())
                                        .build())
            .orElse(null);
    }

    public static String nameFrom(String defendantName, Name name) {
        return Optional.ofNullable(defendantName)
            .orElse(Optional.ofNullable(name)
                .map(Name::getFullName)
                .orElse(null));
    }

    public static CourtCase merge(Case incomingCase, CourtCase existingCourtCase) {
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
            .probationStatus(existingCourtCase.getProbationStatusActual())
            .probationStatusActual(existingCourtCase.getProbationStatusActual())
            .suspendedSentenceOrder(existingCourtCase.getSuspendedSentenceOrder())
            .pnc(existingCourtCase.getPnc())

            .build();
    }

    public static CourtCase merge(ProbationStatusDetail probationStatusDetail, CourtCase existingCourtCase) {
        return CourtCase.builder()
            // Fields to be replaced from new probation status detail
            .breach(probationStatusDetail.getInBreach())
            .preSentenceActivity(probationStatusDetail.isPreSentenceActivity())
            .previouslyKnownTerminationDate(probationStatusDetail.getPreviouslyKnownTerminationDate())
            .probationStatus(probationStatusDetail.getStatus())
            // PK fields
            .courtCode(existingCourtCase.getCourtCode())
            .caseNo(existingCourtCase.getCaseNo())
            // Fields to be retained
            .caseId(String.valueOf(existingCourtCase.getCaseId()))
            .crn(existingCourtCase.getCrn())
            .cro(existingCourtCase.getCro())
            .courtRoom(existingCourtCase.getCourtRoom())
            .defendantAddress(existingCourtCase.getDefendantAddress())
            .name(existingCourtCase.getName())
            .defendantName(existingCourtCase.getDefendantName())
            .defendantSex(existingCourtCase.getDefendantSex())
            .defendantDob(existingCourtCase.getDefendantDob())
            .defendantType(existingCourtCase.getDefendantType())
            .listNo(existingCourtCase.getListNo())
            .pnc(existingCourtCase.getPnc())
            .sessionStartTime(existingCourtCase.getSessionStartTime())
            .suspendedSentenceOrder(existingCourtCase.getSuspendedSentenceOrder())
            .offences(existingCourtCase.getOffences())
            .nationality1(existingCourtCase.getNationality1())
            .nationality2(existingCourtCase.getNationality2())
            .build();
    }

    public static CourtCase newFromCourtCaseWithMatches(CourtCase incomingCase, MatchDetails matchDetails) {

        CourtCaseBuilder courtCaseBuilder = newFromCourtCase(incomingCase)
            .groupedOffenderMatches(buildGroupedOffenderMatch(matchDetails.getMatches(), matchDetails.getMatchType()));

        if (matchDetails.isExactMatch()) {
            Offender offender = matchDetails.getMatches().get(0).getOffender();
            ProbationStatusDetail probationStatus = offender.getProbationStatus();
            courtCaseBuilder
                .breach(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getInBreach).orElse(null))
                .previouslyKnownTerminationDate(
                    Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getPreviouslyKnownTerminationDate).orElse(null))
                .probationStatus(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getStatus).orElse(null))
                .probationStatusActual(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getStatus).orElse(null))
                .preSentenceActivity(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::isPreSentenceActivity).orElse(false))
                .crn(offender.getOtherIds().getCrn())
                .cro(offender.getOtherIds().getCroNumber())
                .pnc(offender.getOtherIds().getPncNumber())
                .build();
        }

        return courtCaseBuilder.build();
    }

    private static GroupedOffenderMatches buildGroupedOffenderMatch(List<Match> matches, MatchType matchType) {

        if (matches == null || matches.isEmpty()) {
            return GroupedOffenderMatches.builder().matches(Collections.emptyList()).build();
        }
        return GroupedOffenderMatches.builder()
            .matches(matches.stream()
                .map(match -> buildOffenderMatch(matchType, match))
                .collect(Collectors.toList()))
            .build();
    }

    private static OffenderMatch buildOffenderMatch(MatchType matchType, Match match) {
        return OffenderMatch.builder()
            .rejected(false)
            .confirmed(false)
            .matchType(matchType)
            .matchIdentifiers(MatchIdentifiers.builder()
                .pnc(match.getOffender().getOtherIds().getPncNumber())
                .cro(match.getOffender().getOtherIds().getCroNumber())
                .crn(match.getOffender().getOtherIds().getCrn())
                .build())
            .build();
    }
}
