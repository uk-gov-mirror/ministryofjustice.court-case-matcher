package uk.gov.justice.probation.courtcasematcher.restclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.NameHelper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NameHelperTest {

    private NameHelper nameHelper;

    @BeforeEach
    void beforeEach() {
        nameHelper = new NameHelper(List.of("MR", "MRS"));
    }

    @Test
    void givenSingleFirstName_whenGetFirstNameCalled_thenReturnFirstName() {
        assertThat(nameHelper.getFirstName("Mr Arthur MORGAN")).isEqualTo("Arthur");
    }

    @Test
    void givenNameAllInUpperCase_whenGetFirstNameCalled_thenReturnFirstName() {
        assertThat(nameHelper.getSurname("MR JASON DAVID HOLLAND")).isEqualTo("JASON DAVID HOLLAND");
    }

    @Test
    void givenMultipleFirstNames_whenGetFirstNameCalled_thenReturnMultipleFirstNames() {
        assertThat(nameHelper.getFirstName("Mary Beth GASKILL")).isEqualTo("Mary Beth");
    }

    @Test
    void givenNoFirstName_whenGetFirstNameCalled_thenReturnEmptyString() {
        assertThat(nameHelper.getFirstName("O'SHEA")).isEqualTo("");
    }

    @Test
    void givenNoSurname_whenGetFirstNameCalled_thenReturnFirstName() {
        assertThat(nameHelper.getFirstName("Molly")).isEqualTo("Molly");
    }

    @Test
    void givenSingleSurname_whenGetSurnameCalled_thenReturnSurname() {
        assertThat(nameHelper.getSurname("Arthur MORGAN")).isEqualTo("MORGAN");
    }

    @Test
    void givenMultipleSurnames_whenGetSurnameCalled_thenReturnMultipleSurnames() {
        assertThat(nameHelper.getSurname("Dutch VAN DER LINDE")).isEqualTo("VAN DER LINDE");
    }

    @Test
    void givenNoFirstName_whenGetSurnameCalled_thenReturnSurname() {
        assertThat(nameHelper.getSurname("O'SHEA")).isEqualTo("O'SHEA");
    }

    @Test
    void givenNoSurname_whenGetSurnameCalled_thenReturnEmptyString() {
        assertThat(nameHelper.getSurname("molly")).isEqualTo("");
    }

    @Test
    void whenGetNameFromNormalName_thenReturn() {
        String fullName = "Mr Arthur MORGAN";

        Name name = nameHelper.getNameFromFields(fullName);

        assertThat(name.getTitle()).isEqualTo("Mr");
        assertThat(name.getForename1()).isEqualTo("Arthur");
        assertThat(name.getSurname()).isEqualTo("MORGAN");
    }

    @ParameterizedTest
    @CsvSource({"Mr ARTHUR MORGAN,ARTHUR MORGAN",
                "Dame Edna EVERAGE,EVERAGE",
                "MasTER William BROWN,BROWN",
                "Mrs JUDI DENCH,JUDI DENCH"})
    void givenVariousTitles_whenGetSurname_thenReturn(String fullName, String expectedSurname) {

        Name name = nameHelper.getNameFromFields(fullName);

        assertThat(name.getSurname()).isEqualTo(expectedSurname);
    }

    @Test
    void whenGetNameFromSurnameOnly_thenReturn() {
        String fullName = "MORGAN";

        Name name = nameHelper.getNameFromFields(fullName);

        assertThat(name.getTitle()).isNullOrEmpty();
        assertThat(name.getForename1()).isNullOrEmpty();
        assertThat(name.getSurname()).isEqualTo("MORGAN");
    }

    @Test
    void whenGetNameFromMultipleForenames_thenReturn() {
        String fullName = "Arthur Stanley MORGAN";

        Name name = nameHelper.getNameFromFields(fullName);

        assertThat(name.getTitle()).isNullOrEmpty();
        assertThat(name.getForename1()).isEqualTo("Arthur Stanley");
        assertThat(name.getSurname()).isEqualTo("MORGAN");
    }
}
