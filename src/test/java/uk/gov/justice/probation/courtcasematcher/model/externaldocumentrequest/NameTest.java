package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameTest {

    private final Name name = Name.builder()
        .title("Mr.")
        .forename1("David")
        .forename2("Robert")
        .surname("BOWIE")
        .build();

    @DisplayName("When there are two of the three forenames, return names space separated")
    @Test
    void givenNullsInName_whenGetForenames_thenReturn() {
        assertThat(name.getForenames()).isEqualTo("David Robert");
    }

    @DisplayName("When there are no forenames, return an empty string")
    @Test
    void givenNoValues_whenGetForenames_thenReturn() {
        Name name1 = Name.builder()
            .title("Mr.")
            .surname("BOWIE")
            .build();
        assertThat(name1.getForenames()).isEqualTo("");
    }

    @DisplayName("When there is one forename, return that string")
    @Test
    void givenOneForename_whenGetForenames_thenReturn() {
        Name name1 = Name.builder()
            .title("Mr.")
            .forename2("  David ")
            .build();
        assertThat(name1.getForenames()).isEqualTo("David");
    }

    @DisplayName("When there is only spaces in the forename(s), return empty string")
    @Test
    void givenOneForenameWithSpaces_whenGetForenames_thenReturn() {
        Name name1 = Name.builder()
            .title("Mr.")
            .forename2(" ")
            .build();
        assertThat(name1.getForenames()).isEmpty();
    }

    @DisplayName("When there is a name, get the full name as a string")
    @Test
    void whenGetFullname_thenReturn() {
        assertThat(name.getFullName()).isEqualTo("Mr. David Robert BOWIE");
    }
}
