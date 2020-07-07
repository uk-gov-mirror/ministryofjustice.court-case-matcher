package uk.gov.justice.probation.courtcasematcher.model.mapper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.probation.courtcasematcher.application.CaseMapperReference;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.Offence;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Address;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OtherIds;

class CaseMapperTest {

    private static final String DEFAULT_PROBATION_STATUS = "No record";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1969, Month.AUGUST, 26);
    private static final LocalDate DATE_OF_HEARING = LocalDate.of(2020, Month.FEBRUARY, 29);
    private static final LocalTime START_TIME = LocalTime.of(9, 10);
    private static final LocalDateTime SESSION_START_TIME = LocalDateTime.of(2020, Month.FEBRUARY, 29, 9, 10);
    public static final String CRN = "CRN123";
    public static final String CRO = "CRO456";
    public static final String PNC = "PNC789";

    private static CaseMapper caseMapper;

    private Block block;

    private Case aCase;

    @BeforeAll
    static void beforeAll() {
        CaseMapperReference caseMapperReference = new CaseMapperReference();
        caseMapperReference.setDefaultProbationStatus(DEFAULT_PROBATION_STATUS);
        caseMapperReference.setCourtNameToCodes(Map.of("SheffieldMagistratesCourt", "SHF"));
        caseMapper = new CaseMapper(caseMapperReference);
    }

    @BeforeEach
    void beforeEach() {
        Session session = Session.builder()
            .courtCode("SHF")
            .courtRoom("00")
            .dateOfHearing(DATE_OF_HEARING)
            .start(START_TIME)
            .build();

        block = Block.builder()
            .session(session)
            .build();

        aCase = Case.builder()
            .block(block)
            .caseNo("123")
            .def_addr(Address.builder().line1("line 1").line2("line 2").line3("line 3").pcode("LD1 1AA").build())
            .def_age("13")
            .def_dob(DATE_OF_BIRTH)
            .def_name("Mr James BLUNT")
            .def_sex("M")
            .def_type("C")
            .id(321321L)
            .listNo("1st")
            .seq(1)
            .offences(singletonList(buildOffence("NEW Theft from a person", 1)))
            .build();
    }

    @DisplayName("Map a new case from gateway case but with no offences")
    @Test
    void whenMapNewCaseThenCreateNewCaseNoOffences() {

        ReflectionTestUtils.setField(aCase, "offences", null);
        CourtCase courtCase = caseMapper.newFromCase(aCase);

        assertThat(courtCase.getCaseNo()).isEqualTo("123");
        assertThat(courtCase.getCaseId()).isEqualTo("321321");
        assertThat(courtCase.getCourtCode()).isEqualTo("SHF");
        assertThat(courtCase.getCourtRoom()).isEqualTo("00");
        assertThat(courtCase.getProbationStatus()).isEqualTo(DEFAULT_PROBATION_STATUS);
        assertThat(courtCase.getDefendantAddress().getLine1()).isEqualTo("line 1");
        assertThat(courtCase.getDefendantAddress().getLine2()).isEqualTo("line 2");
        assertThat(courtCase.getDefendantAddress().getLine3()).isEqualTo("line 3");
        assertThat(courtCase.getDefendantAddress().getPostcode()).isEqualTo("LD1 1AA");
        assertThat(courtCase.getDefendantDob()).isEqualTo(DATE_OF_BIRTH);
        assertThat(courtCase.getDefendantName()).isEqualTo("Mr James BLUNT");
        assertThat(courtCase.getDefendantSex()).isEqualTo("M");
        assertThat(courtCase.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
        assertThat(courtCase.getOffences()).isEmpty();
    }

    @DisplayName("Map a new case from gateway case but with no offences")
    @Test
    void whenMapNewFromCaseAndOffender_thenCreateNewCaseWithOffenderData() {

        ReflectionTestUtils.setField(aCase, "offences", null);
        GroupedOffenderMatches matches = GroupedOffenderMatches.builder()
            .matches(singletonList(
                        OffenderMatch.builder()
                            .matchType(MatchType.NAME_DOB)
                            .build()))
            .build();
        CourtCase courtCase = caseMapper.newFromCaseAndOffender(aCase, Offender.builder()
                .otherIds(OtherIds.builder()
                        .crn(CRN)
                        .cro(CRO)
                        .pnc(PNC)
                        .build())
                .build(), matches);

        assertThat(courtCase.getCrn()).isEqualTo(CRN);
        assertThat(courtCase.getCro()).isEqualTo(CRO);
        assertThat(courtCase.getPnc()).isEqualTo(PNC);

        assertThat(courtCase.getCaseNo()).isEqualTo("123");
        assertThat(courtCase.getCaseId()).isEqualTo("321321");
        assertThat(courtCase.getCourtCode()).isEqualTo("SHF");
        assertThat(courtCase.getCourtRoom()).isEqualTo("00");
        assertThat(courtCase.getProbationStatus()).isEqualTo(DEFAULT_PROBATION_STATUS);
        assertThat(courtCase.getDefendantAddress().getLine1()).isEqualTo("line 1");
        assertThat(courtCase.getDefendantAddress().getLine2()).isEqualTo("line 2");
        assertThat(courtCase.getDefendantAddress().getLine3()).isEqualTo("line 3");
        assertThat(courtCase.getDefendantAddress().getPostcode()).isEqualTo("LD1 1AA");
        assertThat(courtCase.getDefendantDob()).isEqualTo(DATE_OF_BIRTH);
        assertThat(courtCase.getDefendantName()).isEqualTo("Mr James BLUNT");
        assertThat(courtCase.getDefendantSex()).isEqualTo("M");
        assertThat(courtCase.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
        assertThat(courtCase.getOffences()).isEmpty();
        assertThat(courtCase.getGroupedOffenderMatches().getMatches()).hasSize(1);
    }

    @DisplayName("Map from a new case composed of nulls. Ensures no null pointers.")
    @Test
    void whenMapCaseWithNullsThenCreateNewCaseNoOffences_EnsureNoNullPointer() {
        Case nullCase = Case.builder()
            .block(block)
            .build();
        assertThat(caseMapper.newFromCase(nullCase)).isNotNull();
    }

    @DisplayName("Map from a new case with offences")
    @Test
    void whenMapCaseWithOffences_ThenCreateNewCase() {

        uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence offence1 = uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence
            .builder()
            .as("Contrary to section 2(2) and 8 of the Theft Act 1968.")
            .sum("On 02/02/2022 at Town, stole Article, to the value of £0.02, belonging to Person.")
            .title("Theft from a person")
            .seq(1)
            .build();
        uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence offence2 = uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence
            .builder()
            .as("Contrary to section 1(1) and 7 of the Theft Act 1968.")
            .sum("On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Shop.")
            .title("Theft from a shop")
            .seq(2)
            .build();

        // Put Seq 2 first in list
        Case aCase = Case.builder()
            .caseNo("123")
            .block(block)
            .offences(Arrays.asList(offence2, offence1))
            .build();

        CourtCase courtCase = caseMapper.newFromCase(aCase);

        assertThat(courtCase.getOffences()).hasSize(2);
        Offence offence = courtCase.getOffences().get(0);
        assertThat(offence.getSequenceNumber()).isEqualTo(1);
        assertThat(offence.getAct()).isEqualTo("Contrary to section 2(2) and 8 of the Theft Act 1968.");
        assertThat(offence.getOffenceSummary()).isEqualTo("On 02/02/2022 at Town, stole Article, to the value of £0.02, belonging to Person.");
        assertThat(offence.getOffenceTitle()).isEqualTo("Theft from a person");
    }

    @DisplayName("Merge the gateway case with the existing court case, including offences")
    @Test
    void whenMergeWithExistingCase_ThenUpdateExistingCase() {

        CourtCase existingCourtCase = CourtCase.builder()
            .breach(Boolean.TRUE)
            .suspendedSentenceOrder(Boolean.TRUE)
            .crn("X320741")
            .pnc("PNC")
            .caseNo("12345")
            .caseId("123456")
            .probationStatus("Current")
            .courtCode("SHF")
            .defendantAddress(null)
            .defendantName("Pat Garrett")
            .defendantDob(LocalDate.of(1969, Month.JANUARY, 1))
            .nationality1("USA")
            .nationality2("Irish")
            .defendantSex("N")
            .listNo("999st")
            .courtRoom("4")
            .sessionStartTime(LocalDateTime.of(2020, Month.JANUARY, 3, 9, 10, 0))
            .offences(singletonList(Offence.builder()
                                                        .act("act")
                                                        .sequenceNumber(1)
                                                        .offenceSummary("summary")
                                                        .offenceTitle("title")
                                                        .build()))
            .build();

        ReflectionTestUtils.setField(aCase, "def_dob", null);

        CourtCase courtCase = caseMapper.merge(aCase, existingCourtCase);

        // Fields that stay the same on existing value
        assertThat(courtCase.getCourtCode()).isEqualTo("SHF");
        assertThat(courtCase.getProbationStatus()).isEqualTo("Current");
        assertThat(courtCase.getCaseNo()).isEqualTo("12345");
        assertThat(courtCase.getBreach()).isTrue();
        assertThat(courtCase.getSuspendedSentenceOrder()).isTrue();
        assertThat(courtCase.getCrn()).isEqualTo("X320741");
        assertThat(courtCase.getPnc()).isEqualTo("PNC");
        // Fields that get overwritten from Libra incoming (even if null)
        assertThat(courtCase.getCaseId()).isEqualTo("321321");
        assertThat(courtCase.getCourtRoom()).isEqualTo("00");
        assertThat(courtCase.getDefendantAddress().getLine1()).isEqualTo("line 1");
        assertThat(courtCase.getDefendantAddress().getLine2()).isEqualTo("line 2");
        assertThat(courtCase.getDefendantAddress().getLine3()).isEqualTo("line 3");
        assertThat(courtCase.getDefendantAddress().getPostcode()).isEqualTo("LD1 1AA");
        assertThat(courtCase.getDefendantDob()).isNull();
        assertThat(courtCase.getDefendantName()).isEqualTo("Mr James BLUNT");
        assertThat(courtCase.getDefendantSex()).isEqualTo("M");
        assertThat(courtCase.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
        assertThat(courtCase.getNationality1()).isNull();
        assertThat(courtCase.getNationality2()).isNull();
        assertThat(courtCase.getOffences()).hasSize(1);
        assertThat(courtCase.getOffences().get(0).getOffenceTitle()).isEqualTo("NEW Theft from a person");
        assertThat(courtCase.getOffences().get(0).getSequenceNumber()).isEqualTo(1);
    }

    private uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence buildOffence(String title, Integer seq) {
        return uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence.builder()
            .as("Contrary to section 2(2) and 8 of the Theft Act 1968.")
            .sum("On 02/02/2022 at Town, stole Article, to the value of £0.02, belonging to Person.")
            .title(title)
            .seq(seq)
            .build();
    }

}
