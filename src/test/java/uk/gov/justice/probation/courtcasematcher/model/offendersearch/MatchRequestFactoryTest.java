package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchRequestFactoryTest {

    private static final String PNC = "PNC";
    private static final String SURNAME = "SURNAME";
    private static final String FORENAME_1 = "forename1";
    private static final String FORENAME_2 = "forename2";
    private static final String FORENAME_3 = "forename3";
    private static final String TITLE = "Mr";
    private static final Name COMPLETE_NAME = Name.builder()
            .forename1(FORENAME_1)
            .forename2(FORENAME_2)
            .forename3(FORENAME_3)
            .surname(SURNAME)
            .title(TITLE)
            .build();
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1980, 1, 1);
    private static final String DEFENDANT_NAME = "defendantName";

    @Mock
    private NameHelper nameHelper;

    private MatchRequest.Factory factory;

    @BeforeEach
    public void setUp() {
        factory = new MatchRequest.Factory(nameHelper, false);
    }

    @DisplayName("Given flag to use DOB with PNC, then DOB and PNC are in the request.")
    @Test
    public void givenFlagUseDobWithPnc_whenPncAndDobProvided_thenRequestHasDobAndPnc() {
        factory.setUseDobWithPnc(true);
        final var matchRequest = factory.buildFrom(PNC, COMPLETE_NAME, DATE_OF_BIRTH);
        assertThat(matchRequest.getPncNumber()).isEqualTo(PNC);
        assertThat(matchRequest.getFirstName()).isEqualTo(String.format("%s %s %s", FORENAME_1, FORENAME_2, FORENAME_3));
        assertThat(matchRequest.getSurname()).isEqualTo(SURNAME);
        assertThat(matchRequest.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH.format(DateTimeFormatter.ISO_DATE));
    }

    @DisplayName("Given flag to use DOB with PNC, PNC but no DOB set, then PNC but no DOB is in the request.")
    @Test
    public void givenFlagUseDobWithPnc_whenPncButNoDobProvided_thenBuildValidRequest() {
        factory.setUseDobWithPnc(true);
        final var matchRequest = factory.buildFrom(null, COMPLETE_NAME, DATE_OF_BIRTH);
        assertThat(matchRequest.getPncNumber()).isNull();
        assertThat(matchRequest.getFirstName()).isEqualTo(String.format("%s %s %s", FORENAME_1, FORENAME_2, FORENAME_3));
        assertThat(matchRequest.getSurname()).isEqualTo(SURNAME);
        assertThat(matchRequest.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH.format(DateTimeFormatter.ISO_DATE));
    }

    @DisplayName("Given flag to use DOB with PNC, but no PNC set, then DOB is in the request.")
    @Test
    public void givenFlagUseDobWithPnc_whenNoPncProvided_thenRequestHasDob() {
        factory.setUseDobWithPnc(true);
        final var matchRequest = factory.buildFrom(null, COMPLETE_NAME, DATE_OF_BIRTH);
        assertThat(matchRequest.getPncNumber()).isNull();
        assertThat(matchRequest.getFirstName()).isEqualTo(String.format("%s %s %s", FORENAME_1, FORENAME_2, FORENAME_3));
        assertThat(matchRequest.getSurname()).isEqualTo(SURNAME);
        assertThat(matchRequest.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH.format(DateTimeFormatter.ISO_DATE));
    }

    @DisplayName("Given false flag to use DOB with PNC, PNC and DOB set, then no DOB is in the request.")
    @Test
    public void givenFalseFlagUseDobWithPnc_whenPncAndDobProvided_thenRequestHasNoDob() {
        factory.setUseDobWithPnc(false);
        final var matchRequest = factory.buildFrom(PNC, COMPLETE_NAME, DATE_OF_BIRTH);
        assertThat(matchRequest.getPncNumber()).isEqualTo(PNC);
        assertThat(matchRequest.getFirstName()).isEqualTo(String.format("%s %s %s", FORENAME_1, FORENAME_2, FORENAME_3));
        assertThat(matchRequest.getSurname()).isEqualTo(SURNAME);
        assertThat(matchRequest.getDateOfBirth()).isNull();
    }

    @DisplayName("Given false flag to use DOB with PNC, PNC but no DOB set, then no DOB is in the request.")
    @Test
    public void givenFalseFlagUseDobWithPnc_whenPncAndNoDobProvided_thenRequestHasNoDob() {
        factory.setUseDobWithPnc(false);
        final var matchRequest = factory.buildFrom(PNC, COMPLETE_NAME, null);
        assertThat(matchRequest.getPncNumber()).isEqualTo(PNC);
        assertThat(matchRequest.getFirstName()).isEqualTo(String.format("%s %s %s", FORENAME_1, FORENAME_2, FORENAME_3));
        assertThat(matchRequest.getSurname()).isEqualTo(SURNAME);
        assertThat(matchRequest.getDateOfBirth()).isNull();
    }

    @DisplayName("Given false flag to use DOB with PNC, no PNC but DOB is set, then DOB is in the request.")
    @Test
    public void givenFalseFlagUseDobWithPnc_whenNoPncButDobProvided_thenRequestHasDob() {
        factory.setUseDobWithPnc(false);
        final var matchRequest = factory.buildFrom(null, COMPLETE_NAME, DATE_OF_BIRTH);
        assertThat(matchRequest.getPncNumber()).isNull();
        assertThat(matchRequest.getFirstName()).isEqualTo(String.format("%s %s %s", FORENAME_1, FORENAME_2, FORENAME_3));
        assertThat(matchRequest.getSurname()).isEqualTo(SURNAME);
        assertThat(matchRequest.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH.format(DateTimeFormatter.ISO_DATE));
    }

    @DisplayName("Given minimal input of surname only then request is built.")
    @Test
    public void givenMinimalValuesProvided_thenBuildValidRequest() {
        final var name = Name.builder()
                .surname(SURNAME)
                .build();

        final var matchRequest = factory.buildFrom(null, name, null);
        assertThat(matchRequest.getPncNumber()).isEqualTo(null);
        assertThat(matchRequest.getFirstName()).isEqualTo(null);
        assertThat(matchRequest.getSurname()).isEqualTo(SURNAME);
        assertThat(matchRequest.getDateOfBirth()).isNull();
    }

    @DisplayName("Given no surname then exception is thrown.")
    @Test
    public void givenNoSurnameProvided_shouldThrowException() {
        final var name = Name.builder()
                .build();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> factory.buildFrom(null, name, DATE_OF_BIRTH));
    }

    @DisplayName("Given a valid court case but flag indicates not to include DOB then build a request.")
    @Test
    public void givenNameIsProvidedAndFalseFlagToUseDob_whenBuildFromCourtCase_thenBuildRequestWithNoDOB() {
        final var courtCase = CourtCase.builder()
                .name(COMPLETE_NAME)
                .pnc(PNC)
                .defendantDob(DATE_OF_BIRTH)
                .build();
        final var matchRequest = factory.buildFrom(courtCase);
        assertThat(matchRequest.getPncNumber()).isEqualTo(PNC);
        assertThat(matchRequest.getFirstName()).isEqualTo(String.format("%s %s %s", FORENAME_1, FORENAME_2, FORENAME_3));
        assertThat(matchRequest.getSurname()).isEqualTo(SURNAME);
        assertThat(matchRequest.getDateOfBirth()).isNull();
        verifyNoMoreInteractions(nameHelper);
    }

    @DisplayName("Given a valid court case then build a request which includes DOB.")
    @Test
    public void givenNameIsProvided_whenBuildFromCourtCase_thenBuildValidRequest() {
        factory.setUseDobWithPnc(true);
        final var courtCase = CourtCase.builder()
            .name(COMPLETE_NAME)
            .pnc(PNC)
            .defendantDob(DATE_OF_BIRTH)
            .build();
        final var matchRequest = factory.buildFrom(courtCase);
        assertThat(matchRequest.getPncNumber()).isEqualTo(PNC);
        assertThat(matchRequest.getFirstName()).isEqualTo(String.format("%s %s %s", FORENAME_1, FORENAME_2, FORENAME_3));
        assertThat(matchRequest.getSurname()).isEqualTo(SURNAME);
        assertThat(matchRequest.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH.format(DateTimeFormatter.ISO_DATE));
        verifyNoMoreInteractions(nameHelper);
    }

    @DisplayName("Given no complex name provided then fall back to name helper to construct.")
    @Test
    public void givenNoNameProvided_whenBuildFromCourtCase_thenBuildValidRequest() {
        when(nameHelper.getNameFromFields(DEFENDANT_NAME)).thenReturn(COMPLETE_NAME);

        final var courtCase = CourtCase.builder()
                .defendantName(DEFENDANT_NAME)
                .defendantDob(DATE_OF_BIRTH)
                .build();
        final var matchRequest = factory.buildFrom(courtCase);
        assertThat(matchRequest.getPncNumber()).isNull();
        assertThat(matchRequest.getFirstName()).isEqualTo(String.format("%s %s %s", FORENAME_1, FORENAME_2, FORENAME_3));
        assertThat(matchRequest.getSurname()).isEqualTo(SURNAME);
        assertThat(matchRequest.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH.format(DateTimeFormatter.ISO_DATE));
        verifyNoMoreInteractions(nameHelper);
    }
}
