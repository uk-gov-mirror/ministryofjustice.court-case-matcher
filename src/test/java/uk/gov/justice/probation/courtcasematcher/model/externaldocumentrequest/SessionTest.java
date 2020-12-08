package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTest {

    @Test
    void whenOuCodeNotSession_thenReturnFromParentInfo() {
        Info info = Info.builder().ouCode("B10JQ").build();
        Document document = Document.builder().info(info).build();
        DataJob dataJob = DataJob.builder().document(document).build();
        Job job = Job.builder().dataJob(dataJob).build();
        Session session = Session.builder().job(job).build();

        assertThat(session.getCourtCode()).isEqualTo("B10JQ");
    }

    @Test
    void whenOuCodeIsInSession_thenReturn() {
        Session session = Session.builder().courtCode("B10JQ").build();

        assertThat(session.getCourtCode()).isEqualTo("B10JQ");
    }

    @Test
    void getSessionStartTime() {
        LocalDate dateOfHearing = LocalDate.of(2020, Month.JULY, 19);
        Session session = Session.builder()
            .dateOfHearing(dateOfHearing)
            .start(LocalTime.NOON)
            .build();

        assertThat(session.getSessionStartTime()).isEqualTo(LocalDateTime.of(2020, Month.JULY, 19, 12, 0, 0));
    }
}
