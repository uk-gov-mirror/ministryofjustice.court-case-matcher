package uk.gov.justice.probation.courtcasematcher.restclient;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameHelperTest {

    @Test
    public void givenSingleFirstName_whenGetFirstNameCalled_thenReturnFirstName() {
        assertThat(NameHelper.getFirstName("Arthur MORGAN")).isEqualTo("Arthur");
    }

    @Test
    public void givenMultipleFirstNames_whenGetFirstNameCalled_thenReturnMultipleFirstNames() {
        assertThat(NameHelper.getFirstName("Mary Beth GASKILL")).isEqualTo("Mary Beth");
    }

    @Test
    public void givenNoFirstName_whenGetFirstNameCalled_thenReturnEmptyString() {
        assertThat(NameHelper.getFirstName("O'SHEA")).isEqualTo("");
    }

    @Test
    public void givenNoSurname_whenGetFirstNameCalled_thenReturnFirstName() {
        assertThat(NameHelper.getFirstName("Molly")).isEqualTo("Molly");
    }

    @Test
    public void givenSingleSurname_whenGetSurnameCalled_thenReturnSurname() {
        assertThat(NameHelper.getSurname("Arthur MORGAN")).isEqualTo("MORGAN");
    }

    @Test
    public void givenMultipleSurnames_whenGetSurnameCalled_thenReturnMultipleSurnames() {
        assertThat(NameHelper.getSurname("Dutch VAN DER LINDE")).isEqualTo("VAN DER LINDE");
    }

    @Test
    public void givenNoFirstName_whenGetSurnameCalled_thenReturnSurname() {
        assertThat(NameHelper.getSurname("O'SHEA")).isEqualTo("O'SHEA");
    }

    @Test
    public void givenNoSurname_whenGetSurnameCalled_thenReturnEmptyString() {
        assertThat(NameHelper.getSurname("molly")).isEqualTo("");
    }

}