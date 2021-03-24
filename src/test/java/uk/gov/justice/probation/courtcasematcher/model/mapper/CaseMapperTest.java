package uk.gov.justice.probation.courtcasematcher.model.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.Offence;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Address;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.DataJob;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Document;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Job;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OtherIds;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.service.SearchResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class CaseMapperTest {

    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1969, Month.AUGUST, 26);
    private static final LocalDate DATE_OF_HEARING = LocalDate.of(2020, Month.FEBRUARY, 29);
    private static final LocalTime START_TIME = LocalTime.of(9, 10);
    private static final LocalDateTime SESSION_START_TIME = LocalDateTime.of(2020, Month.FEBRUARY, 29, 9, 10);
    private static final String COURT_CODE = "B10JQ";
    private static final String COURT_NAME = "North Shields Magistrates Court";

    private static final Name name = Name.builder().title("Mr")
                                                    .forename1("Patrick")
                                                    .forename2("Floyd")
                                                    .forename3("Jarvis")
                                                    .surname("Garrett")
                                                    .build();
    public static final String CRN = "CRN123";
    public static final String CRO = "CRO456";
    public static final String PNC = "PNC789";

    private Block block;

    private Case aCase;

    @BeforeEach
    void beforeEach() {

        Info info = Info.builder().ouCode(COURT_CODE).dateOfHearing(DATE_OF_HEARING).build();
        Document document = Document.builder().info(info).build();
        DataJob dataJob = DataJob.builder().document(document).build();
        Job job = Job.builder().dataJob(dataJob).build();

        Session session = Session.builder()
            .courtName(COURT_NAME)
            .courtRoom("00")
            .dateOfHearing(DATE_OF_HEARING)
            .start(START_TIME)
            .job(job)
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
            .name(name)
            .def_sex("M")
            .def_type("P")
            .id(321321L)
            .listNo("1st")
            .seq(1)
            .offences(singletonList(buildOffence("NEW Theft from a person", 1)))
            .build();
    }

    @DisplayName("New from an existing CourtCase, adding MatchDetails")
    @Nested
    class NewFromCourtCaseWithMatches {

        @DisplayName("Map a court case to a new court case when search response has yielded no matches")
        @Test
        void givenNoMatches_whenMapNewFromCaseAndSearchResponse_thenCreateNewCaseWithEmptyListOfMatches() {

            CourtCase courtCase = CaseMapper.newFromCase(aCase);
            SearchResponse searchResponse = SearchResponse.builder()
                .matchedBy(OffenderSearchMatchType.NOTHING)
                .build();

            CourtCase courtCaseNew = CaseMapper.newFromCourtCaseWithMatches(courtCase, buildMatchDetails(searchResponse));

            assertThat(courtCaseNew).isNotSameAs(courtCase);
            assertThat(courtCaseNew.getCrn()).isNull();
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).hasSize(0);
        }

        @DisplayName("Map a court case to a new court case when search response has yielded a single match")
        @Test
        void givenSingleMatch_whenMapNewFromCaseAndSearchResponse_thenCreateNewCaseWithSingleMatch() {
            Match match = Match.builder()
                .offender(Offender.builder()
                    .otherIds(OtherIds.builder().crn(CRN).croNumber(CRO).pncNumber(PNC).build())
                    .probationStatus(ProbationStatusDetail.builder().status("CURRENT").preSentenceActivity(true).build())
                    .build())
                .build();

            CourtCase courtCase = CaseMapper.newFromCase(aCase);
            SearchResponse searchResponse = SearchResponse.builder()
                .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
                .matches(List.of(match))
                .build();

            CourtCase courtCaseNew = CaseMapper.newFromCourtCaseWithMatches(courtCase, buildMatchDetails(searchResponse));

            assertThat(courtCaseNew).isNotSameAs(courtCase);
            assertThat(courtCaseNew.getCrn()).isEqualTo(CRN);
            assertThat(courtCaseNew.getPnc()).isEqualTo(PNC);
            assertThat(courtCaseNew.getProbationStatus()).isEqualTo("CURRENT");
            assertThat(courtCaseNew.getPreviouslyKnownTerminationDate()).isNull();
            assertThat(courtCaseNew.getBreach()).isNull();
            assertThat(courtCaseNew.isPreSentenceActivity()).isTrue();
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).hasSize(1);
            OffenderMatch offenderMatch1 = buildOffenderMatch(MatchType.NAME_DOB, CRN, CRO, PNC);
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).containsExactly(offenderMatch1);
        }

        @DisplayName("Map a court case to a new court case when search response has yielded a single match")
        @Test
        void givenSingleMatchOnName_whenMapNewFromCaseAndSearchResponse_thenCreateNewCaseWithSingleMatchButNoCrn() {
            Match match = Match.builder().offender(Offender.builder()
                .otherIds(OtherIds.builder().crn(CRN).croNumber(CRO).pncNumber(PNC).build())
                .build())
                .build();

            CourtCase courtCase = CaseMapper.newFromCase(aCase);
            SearchResponse searchResponse = SearchResponse.builder()
                .matchedBy(OffenderSearchMatchType.NAME)
                .matches(List.of(match))
                .build();

            CourtCase courtCaseNew = CaseMapper.newFromCourtCaseWithMatches(courtCase, buildMatchDetails(searchResponse));

            assertThat(courtCaseNew).isNotSameAs(courtCase);
            assertThat(courtCaseNew.getCrn()).isNull();
            assertThat(courtCaseNew.getProbationStatus()).isNull();
            assertThat(courtCaseNew.getBreach()).isNull();
            assertThat(courtCaseNew.getPreviouslyKnownTerminationDate()).isNull();
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).hasSize(1);
            OffenderMatch expectedOffenderMatch = buildOffenderMatch(MatchType.NAME, CRN, CRO, PNC);
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).containsExactly(expectedOffenderMatch);
        }

        @DisplayName("Map a court case to a new court case when search response has yielded a single match but null probation status")
        @Test
        void givenSingleMatchWithNoProbationStatus_whenMapNewFromCaseAndSearchResponse_thenCreateNewCaseWithSingleMatch() {
            Match match = Match.builder().offender(Offender.builder()
                .otherIds(OtherIds.builder().crn(CRN).croNumber(CRO).pncNumber(PNC).build())
                .build())
                .build();

            CourtCase courtCase = CaseMapper.newFromCase(aCase);
            SearchResponse searchResponse = SearchResponse.builder()
                .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
                .matches(List.of(match))
                .build();

            CourtCase courtCaseNew = CaseMapper.newFromCourtCaseWithMatches(courtCase, buildMatchDetails(searchResponse));

            assertThat(courtCaseNew).isNotSameAs(courtCase);
            assertThat(courtCaseNew.getCrn()).isEqualTo(CRN);
            assertThat(courtCaseNew.getProbationStatus()).isNull();
            assertThat(courtCaseNew.getBreach()).isNull();
            assertThat(courtCaseNew.getPreviouslyKnownTerminationDate()).isNull();
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).hasSize(1);
            OffenderMatch offenderMatch1 = buildOffenderMatch(MatchType.NAME_DOB, CRN, CRO, PNC);
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).containsExactly(offenderMatch1);
        }

        @DisplayName("Map a court case to a new court case when search response has yielded multiple matches")
        @Test
        void givenMultipleMatches_whenMapNewFromCaseAndSearchResponse_thenCreateNewCaseWithListOfMatches() {
            Match match1 = Match.builder().offender(Offender.builder()
                .otherIds(OtherIds.builder().crn(CRN).croNumber(CRO).pncNumber(PNC).build())
                .build())
                .build();
            Match match2 = Match.builder().offender(Offender.builder()
                .otherIds(OtherIds.builder().crn("CRN1").build())
                .build())
                .build();

            CourtCase courtCase = CaseMapper.newFromCase(aCase);
            SearchResponse searchResponse = SearchResponse.builder()
                .matchedBy(OffenderSearchMatchType.PARTIAL_NAME)
                .matches(List.of(match1, match2))
                .build();

            CourtCase courtCaseNew = CaseMapper.newFromCourtCaseWithMatches(courtCase, buildMatchDetails(searchResponse));

            assertThat(courtCaseNew).isNotSameAs(courtCase);
            assertThat(courtCaseNew.getCrn()).isNull();
            assertThat(courtCaseNew.getProbationStatus()).isNull();
            assertThat(courtCaseNew.getBreach()).isNull();
            assertThat(courtCaseNew.getPreviouslyKnownTerminationDate()).isNull();
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).hasSize(2);
            OffenderMatch offenderMatch1 = buildOffenderMatch(MatchType.PARTIAL_NAME, CRN, CRO, PNC);
            OffenderMatch offenderMatch2 = buildOffenderMatch(MatchType.PARTIAL_NAME, "CRN1", null, null);
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).containsExactlyInAnyOrder(offenderMatch1, offenderMatch2);
        }

        private MatchDetails buildMatchDetails(SearchResponse searchResponse) {
            return MatchDetails.builder()
                .matchType(MatchType.of(SearchResult.builder()
                    .searchResponse(searchResponse)
                    .build()))
                .matches(searchResponse.getMatches())
                .exactMatch(searchResponse.isExactMatch())
                .build();
        }

        private OffenderMatch buildOffenderMatch(MatchType matchType, String crn, String cro, String pnc) {
            return OffenderMatch.builder()
                .matchType(matchType)
                .confirmed(false)
                .rejected(false)
                .matchIdentifiers(MatchIdentifiers.builder().pnc(pnc).cro(cro).crn(crn).build())
                .build();
        }
    }

    @DisplayName("New from incoming gateway case")
    @Nested
    class NewFromGatewayCase {

        @DisplayName("Map from a new case composed of nulls. Ensures no null pointers.")
        @Test
        void whenMapCaseWithNullsThenCreateNewCaseNoOffences_EnsureNoNullPointer() {
            Case nullCase = Case.builder()
                .block(block)
                .build();
            assertThat(CaseMapper.newFromCase(nullCase)).isNotNull();
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

            CourtCase courtCase = CaseMapper.newFromCase(aCase);

            assertThat(courtCase.getOffences()).hasSize(2);
            Offence offence = courtCase.getOffences().get(0);
            assertThat(offence.getSequenceNumber()).isEqualTo(1);
            assertThat(offence.getAct()).isEqualTo("Contrary to section 2(2) and 8 of the Theft Act 1968.");
            assertThat(offence.getOffenceSummary()).isEqualTo("On 02/02/2022 at Town, stole Article, to the value of £0.02, belonging to Person.");
            assertThat(offence.getOffenceTitle()).isEqualTo("Theft from a person");
        }

        @DisplayName("Map a new case from gateway case but with no offences")
        @Test
        void whenMapNewCaseThenCreateNewCaseNoOffences() {

            ReflectionTestUtils.setField(aCase, "offences", null);
            CourtCase courtCase = CaseMapper.newFromCase(aCase);

            assertThat(courtCase.getCaseNo()).isEqualTo("123");
            assertThat(courtCase.getCaseId()).isEqualTo("321321");
            assertThat(courtCase.getCourtCode()).isEqualTo(COURT_CODE);
            assertThat(courtCase.getCourtRoom()).isEqualTo("00");
            assertThat(courtCase.getProbationStatus()).isNull();
            assertThat(courtCase.getDefendantAddress().getLine1()).isEqualTo("line 1");
            assertThat(courtCase.getDefendantAddress().getLine2()).isEqualTo("line 2");
            assertThat(courtCase.getDefendantAddress().getLine3()).isEqualTo("line 3");
            assertThat(courtCase.getDefendantAddress().getPostcode()).isEqualTo("LD1 1AA");
            assertThat(courtCase.getDefendantDob()).isEqualTo(DATE_OF_BIRTH);
            assertThat(courtCase.getDefendantName()).isEqualTo("Mr Patrick Floyd Jarvis Garrett");
            assertThat(courtCase.getName()).isEqualTo(name);
            assertThat(courtCase.getDefendantSex()).isEqualTo("M");
            assertThat(courtCase.getDefendantType()).isSameAs(DefendantType.PERSON);
            assertThat(courtCase.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
            assertThat(courtCase.getOffences()).isEmpty();
        }
    }

    @DisplayName("Merge incoming gateway case to existing CourtCase")
    @Nested
    class MergeGatewayToExistingCourtCase {

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
                .probationStatusActual("CURRENT")
                .courtCode(COURT_CODE)
                .defendantAddress(null)
                .defendantName("Pat Garrett")
                .defendantType(DefendantType.ORGANISATION)
                .defendantDob(LocalDate.of(1969, Month.JANUARY, 1))
                .nationality1("USA")
                .nationality2("Irish")
                .defendantSex("N")
                .listNo("999st")
                .courtRoom("4")
                .previouslyKnownTerminationDate(LocalDate.of(2001, Month.AUGUST, 26))
                .sessionStartTime(LocalDateTime.of(2020, Month.JANUARY, 3, 9, 10, 0))
                .offences(singletonList(Offence.builder()
                    .act("act")
                    .sequenceNumber(1)
                    .offenceSummary("summary")
                    .offenceTitle("title")
                    .build()))
                .build();

            ReflectionTestUtils.setField(aCase, "def_dob", null);

            CourtCase courtCase = CaseMapper.merge(aCase, existingCourtCase);

            // Fields that stay the same on existing value
            assertThat(courtCase.getCourtCode()).isEqualTo(COURT_CODE);
            assertThat(courtCase.getProbationStatus()).isEqualTo("CURRENT");
            assertThat(courtCase.getProbationStatusActual()).isEqualTo("CURRENT");
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
            assertThat(courtCase.getDefendantName()).isEqualTo("Mr Patrick Floyd Jarvis Garrett");
            assertThat(courtCase.getName()).isEqualTo(name);
            assertThat(courtCase.getDefendantType()).isSameAs(DefendantType.PERSON);
            assertThat(courtCase.getDefendantSex()).isEqualTo("M");
            assertThat(courtCase.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
            assertThat(courtCase.getNationality1()).isNull();
            assertThat(courtCase.getNationality2()).isNull();
            assertThat(courtCase.getPreviouslyKnownTerminationDate()).isEqualTo(LocalDate.of(2001, Month.AUGUST, 26));
            assertThat(courtCase.getOffences()).hasSize(1);
            assertThat(courtCase.getOffences().get(0).getOffenceTitle()).isEqualTo("NEW Theft from a person");
            assertThat(courtCase.getOffences().get(0).getSequenceNumber()).isEqualTo(1);
        }
    }

    @DisplayName("Merge ProbationStatusDetail to existing CourtCase")
    @Nested
    class MergeProbationStatusDetailToExistingCourtCase {

        @DisplayName("Merge the gateway case with the existing court case, including offences")
        @Test
        void whenMergeWithExistingCase_ThenUpdateExistingCase() {

            CourtCase existingCourtCase = CourtCase.builder()
                .breach(Boolean.TRUE)
                .suspendedSentenceOrder(Boolean.TRUE)
                .crn(CRN)
                .pnc(PNC)
                .cro(CRO)
                .caseNo("12345")
                .caseId("123456")
                .courtCode(COURT_CODE)
                .defendantAddress(null)
                .defendantName("Pat Garrett")
                .defendantType(DefendantType.PERSON)
                .defendantDob(LocalDate.of(1969, Month.JANUARY, 1))
                .name(Name.builder().forename1("Pat").surname("Garrett").build())
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
                .preSentenceActivity(false)
                .probationStatus("NOT_SENTENCED")
                .breach(false)
                .previouslyKnownTerminationDate(LocalDate.of(2001, Month.AUGUST, 26))
                .build();

            LocalDate nextPrevKnownTermDate = existingCourtCase.getPreviouslyKnownTerminationDate().plusDays(1);
            ProbationStatusDetail probationStatusDetail = ProbationStatusDetail.builder()
                .preSentenceActivity(true)
                .inBreach(Boolean.TRUE)
                .previouslyKnownTerminationDate(nextPrevKnownTermDate)
                .status("CURRENT")
                .build();

            CourtCase courtCase = CaseMapper.merge(probationStatusDetail, existingCourtCase);

            assertThat(courtCase).isNotSameAs(existingCourtCase);

            // Fields that are updated
            assertThat(courtCase.getProbationStatus()).isEqualTo("CURRENT");
            assertThat(courtCase.getPreviouslyKnownTerminationDate()).isEqualTo(nextPrevKnownTermDate);
            assertThat(courtCase.getBreach()).isTrue();
            assertThat(courtCase.isPreSentenceActivity()).isTrue();
            // Fields that stay the same on existing value
            assertThat(courtCase.getCaseId()).isEqualTo(existingCourtCase.getCaseId());
            assertThat(courtCase.getCaseNo()).isEqualTo(existingCourtCase.getCaseNo());
            assertThat(courtCase.getCourtCode()).isEqualTo(existingCourtCase.getCourtCode());
            assertThat(courtCase.getCourtRoom()).isEqualTo(existingCourtCase.getCourtRoom());
            assertThat(courtCase.getCrn()).isEqualTo(existingCourtCase.getCrn());
            assertThat(courtCase.getCro()).isEqualTo(existingCourtCase.getCro());
            assertThat(courtCase.getDefendantAddress()).isEqualTo(existingCourtCase.getDefendantAddress());
            assertThat(courtCase.getDefendantDob()).isEqualTo(existingCourtCase.getDefendantDob());
            assertThat(courtCase.getDefendantName()).isEqualTo(existingCourtCase.getDefendantName());
            assertThat(courtCase.getDefendantSex()).isEqualTo(existingCourtCase.getDefendantSex());
            assertThat(courtCase.getDefendantType()).isSameAs(existingCourtCase.getDefendantType());
            assertThat(courtCase.getListNo()).isEqualTo(existingCourtCase.getListNo());
            assertThat(courtCase.getName()).isEqualTo(existingCourtCase.getName());
            assertThat(courtCase.getNationality1()).isEqualTo(existingCourtCase.getNationality1());
            assertThat(courtCase.getNationality2()).isEqualTo(existingCourtCase.getNationality2());
            assertThat(courtCase.getPnc()).isEqualTo(existingCourtCase.getPnc());
            assertThat(courtCase.getSessionStartTime()).isEqualTo(existingCourtCase.getSessionStartTime());
            assertThat(courtCase.getSuspendedSentenceOrder()).isEqualTo(existingCourtCase.getSuspendedSentenceOrder());
            assertThat(courtCase.getOffences()).hasSize(1);
            assertThat(courtCase.getOffences().get(0).getOffenceTitle()).isEqualTo("title");
            assertThat(courtCase.getOffences().get(0).getSequenceNumber()).isEqualTo(1);
        }
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
