package uk.gov.justice.probation.courtcasematcher.restclient;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;

import static org.assertj.core.api.Assertions.assertThat;

class NameHelperTest {

    @Test
    void givenSingleFirstName_whenGetFirstNameCalled_thenReturnFirstName() {
        assertThat(NameHelper.getFirstName("Arthur MORGAN")).isEqualTo("Arthur");
    }

    @Test
    void givenMultipleFirstNames_whenGetFirstNameCalled_thenReturnMultipleFirstNames() {
        assertThat(NameHelper.getFirstName("Mary Beth GASKILL")).isEqualTo("Mary Beth");
    }

    @Test
    void givenNoFirstName_whenGetFirstNameCalled_thenReturnEmptyString() {
        assertThat(NameHelper.getFirstName("O'SHEA")).isEqualTo("");
    }

    @Test
    void givenNoSurname_whenGetFirstNameCalled_thenReturnFirstName() {
        assertThat(NameHelper.getFirstName("Molly")).isEqualTo("Molly");
    }

    @Test
    void givenSingleSurname_whenGetSurnameCalled_thenReturnSurname() {
        assertThat(NameHelper.getSurname("Arthur MORGAN")).isEqualTo("MORGAN");
    }

    @Test
    void givenMultipleSurnames_whenGetSurnameCalled_thenReturnMultipleSurnames() {
        assertThat(NameHelper.getSurname("Dutch VAN DER LINDE")).isEqualTo("VAN DER LINDE");
    }

    @Test
    void givenNoFirstName_whenGetSurnameCalled_thenReturnSurname() {
        assertThat(NameHelper.getSurname("O'SHEA")).isEqualTo("O'SHEA");
    }

    @Test
    void givenNoSurname_whenGetSurnameCalled_thenReturnEmptyString() {
        assertThat(NameHelper.getSurname("molly")).isEqualTo("");
    }

    @Test
    void whenGetNameFromNormalName_thenReturn() {
        String fullName = "Arthur MORGAN";

        Name name = NameHelper.getNameFromFields(fullName);

        assertThat(name.getForename1()).isEqualTo("Arthur");
        assertThat(name.getSurname()).isEqualTo("MORGAN");
    }

    @Test
    void whenGetNameFromSurnameOnly_thenReturn() {
        String fullName = "MORGAN";

        Name name = NameHelper.getNameFromFields(fullName);

        assertThat(name.getForename1()).isNullOrEmpty();
        assertThat(name.getSurname()).isEqualTo("MORGAN");
    }

    @Test
    void whenGetNameFromMultipleForenames_thenReturn() {
        String fullName = "Arthur Stanley MORGAN";

        Name name = NameHelper.getNameFromFields(fullName);

        assertThat(name.getForename1()).isEqualTo("Arthur Stanley");
        assertThat(name.getSurname()).isEqualTo("MORGAN");
    }
}
