package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InfoTest {

    @DisplayName("Ensures that Lombok generated equality ignores the source detail sequence and protects against inadvertent change.")
    @Test
    void equalityIgnoresInfoSequence() {
        LocalDate now = LocalDate.now();
        Info info1 = Info.builder().ouCode("B16BG00").sequence(1L).dateOfHearing(now).build();
        Info info2 = Info.builder().ouCode("B16BG00").sequence(2L).dateOfHearing(now).build();

        assertThat(info1).isEqualTo(info2);
    }

}
