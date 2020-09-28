package uk.gov.justice.probation.courtcasematcher.restclient;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.eventbus.EventBus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.event.OffenderSearchFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.OffenderSearchValidationFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SuppressWarnings("UnstableApiUsage")
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
public class OffenderSearchResponseRestClientIntTest {

    @Autowired
    private OffenderSearchRestClient restClient;

    @MockBean
    private EventBus eventBus;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .port(8090)
            .stubRequestLoggingDisabled(false)
            .usingFilesUnderClasspath("mocks"));
    private final ArgumentCaptor<OffenderSearchFailureEvent> failureCaptor = ArgumentCaptor.forClass(OffenderSearchFailureEvent.class);
    private final ArgumentCaptor<OffenderSearchValidationFailureEvent> validationFailureCaptor = ArgumentCaptor.forClass(OffenderSearchValidationFailureEvent.class);


    @Test
    public void givenSingleMatchReturned_whenSearch_thenReturnIt() {
        Optional<SearchResponse> match = restClient.search("Arthur MORGAN", LocalDate.of(1975, 1, 1))
                .blockOptional();

        assertThat(match).isPresent();
        assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.ALL_SUPPLIED);
        assertThat(match.get().getMatches().size()).isEqualTo(1);
        assertThat(match.get().isExactMatch()).isTrue();

        Offender offender = match.get().getMatches().get(0).getOffender();
        assertThat(offender.getOtherIds().getCrn()).isEqualTo("X346204");
        assertThat(offender.getOtherIds().getCro()).isEqualTo("1234ABC");
        assertThat(offender.getOtherIds().getPnc()).isEqualTo("ABCD1234");
    }

    @Test
    public void givenSingleMatchNonExactMatchReturned_whenSearch_thenReturnIt() {
        Optional<SearchResponse> match = restClient.search("Calvin HARRIS", LocalDate.of(1969, Month.AUGUST, 26))
            .blockOptional();

        assertThat(match).isPresent();
        assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.NAME);
        assertThat(match.get().getMatches().size()).isEqualTo(1);
        assertThat(match.get().isExactMatch()).isFalse();
    }

    @Test
    public void givenMultipleMatchesReturned_whenSearch_thenReturnThem() {
        Optional<SearchResponse> match = restClient.search("John MARSTON", LocalDate.of(1982, 4, 5))
                .blockOptional();

        assertThat(match).isPresent();
        assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.ALL_SUPPLIED);
        assertThat(match.get().getMatches().size()).isEqualTo(2);

        Offender offender1 = match.get().getMatches().get(0).getOffender();
        assertThat(offender1.getOtherIds().getCrn()).isEqualTo("Y346123");
        assertThat(offender1.getOtherIds().getCro()).isEqualTo("2234DEF");
        assertThat(offender1.getOtherIds().getPnc()).isEqualTo("BBCD1567");

        Offender offender2 = match.get().getMatches().get(1).getOffender();
        assertThat(offender2.getOtherIds().getCrn()).isEqualTo("Z346124");
        assertThat(offender2.getOtherIds().getCro()).isEqualTo("3234DEG");
        assertThat(offender2.getOtherIds().getPnc()).isEqualTo("CBCD1568");
    }

    @Test
    public void givenNoMatchesReturned_whenSearch_thenReturnEmptyList() {
        Optional<SearchResponse> match = restClient.search("Juan MARSTONEZ", LocalDate.of(1982, 4, 5))
                .blockOptional();

        assertThat(match).isPresent();
        assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.NOTHING);
        assertThat(match.get().getMatches().size()).isEqualTo(0);
    }

    @Test
    public void givenUnexpectedError_whenSearch_thenPushErrorEventToBus() {
        Optional<SearchResponse> match = restClient.search("error", LocalDate.of(1982, 4, 5))
                .blockOptional();

        assertThat(match).isEmpty();
        verify(eventBus).post(any(OffenderSearchFailureEvent.class));
        verify(eventBus).post(failureCaptor.capture());
        assertThat(failureCaptor.getValue().getFailureMessage()).isEqualTo("500 Internal Server Error from POST http://localhost:8090/match");
        assertThat(failureCaptor.getValue().getRequestJson()).isEqualTo("{\"firstName\":\"error\",\"surname\":\"\",\"dateOfBirth\":\"1982-04-05\"}");
    }

    @Test
    public void givenUnexpected404_whenSearch_thenPushErrorEventToBus() {
        Optional<SearchResponse> match = restClient.search("not found", LocalDate.of(1999, 4, 5))
                .blockOptional();

        assertThat(match).isEmpty();
        verify(eventBus).post(failureCaptor.capture());
        assertThat(failureCaptor.getValue().getFailureMessage()).isEqualTo("404 Not Found from POST http://localhost:8090/match");
        assertThat(failureCaptor.getValue().getRequestJson()).isEqualTo("{\"firstName\":\"not found\",\"surname\":\"\",\"dateOfBirth\":\"1999-04-05\"}");
    }

    @Test
    public void givenUnexpected401_whenSearch_thenPushErrorEventToBus() {
        Optional<SearchResponse> match = restClient.search("unauthorised", LocalDate.of(1982, 4, 5))
                .blockOptional();

        assertThat(match).isEmpty();
        verify(eventBus).post(failureCaptor.capture());
        assertThat(failureCaptor.getValue().getFailureMessage()).isEqualTo("401 Unauthorized from POST http://localhost:8090/match");
        assertThat(failureCaptor.getValue().getRequestJson()).isEqualTo("{\"firstName\":\"unauthorised\",\"surname\":\"\",\"dateOfBirth\":\"1982-04-05\"}");
    }

    @Test
    public void givenNullDateOfBirth_thenReturnEmptyAndPushErrorEventToBus() {
        Optional<SearchResponse> match = restClient.search("foo", null)
                .blockOptional();

        assertThat(match).isEmpty();
        verify(eventBus).post(any(OffenderSearchValidationFailureEvent.class));
        verify(eventBus).post(validationFailureCaptor.capture());
        assertThat(validationFailureCaptor.getValue().getFailureMessage()).isEqualTo("No dateOfBirth provided");
        assertThat(validationFailureCaptor.getValue().getFullName()).isEqualTo("foo");
        assertThat(validationFailureCaptor.getValue().getDateOfBirth()).isNull();
    }

    @Test
    public void givenNullName_thenReturnEmptyAndPushErrorEventToBus() {
        Optional<SearchResponse> match = restClient.search(null, LocalDate.of(1982, 4, 5))
                .blockOptional();

        assertThat(match).isEmpty();
        verify(eventBus).post(any(OffenderSearchValidationFailureEvent.class));
        verify(eventBus).post(validationFailureCaptor.capture());
        assertThat(validationFailureCaptor.getValue().getFailureMessage()).isEqualTo("No name provided");
        assertThat(validationFailureCaptor.getValue().getFullName()).isEqualTo(null);
        assertThat(validationFailureCaptor.getValue().getDateOfBirth()).isEqualTo(LocalDate.of(1982, 4, 5));
    }

    @Test
    public void givenEmptyName_thenReturnEmptyAndPushErrorEventToBus() {
        Optional<SearchResponse> match = restClient.search("", LocalDate.of(1982, 4, 5))
                .blockOptional();

        assertThat(match).isEmpty();
        verify(eventBus).post(any(OffenderSearchValidationFailureEvent.class));
        verify(eventBus).post(validationFailureCaptor.capture());
        assertThat(validationFailureCaptor.getValue().getFailureMessage()).isEqualTo("No name provided");
        assertThat(validationFailureCaptor.getValue().getFullName()).isEqualTo("");
        assertThat(validationFailureCaptor.getValue().getDateOfBirth()).isEqualTo(LocalDate.of(1982, 4, 5));
    }

}
