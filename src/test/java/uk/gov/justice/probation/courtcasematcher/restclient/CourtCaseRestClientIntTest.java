package uk.gov.justice.probation.courtcasematcher.restclient;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.eventbus.EventBus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseSuccessEvent;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.Address;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.Offence;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class CourtCaseRestClientIntTest {

    private static final String COURT_CODE = "SHF";
    private static final String CASE_NO = "12345";
    private static final String NEW_CASE_NO = "999999";
    private static final int WEB_CLIENT_TIMEOUT_MS = 2000;

    private static final CourtCase A_CASE = CourtCase.builder()
        .caseId("1246257")
        .caseNo(CASE_NO)
        .courtCode(COURT_CODE)
        .build();

    @MockBean
    private EventBus eventBus;

    @Autowired
    private CourtCaseRestClient restClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .port(8090)
            .usingFilesUnderClasspath("mocks"));

    @Test
    public void whenGetCourtCase_thenMakeRestCallToCourtCaseService() {

        LocalDateTime startTime = LocalDateTime.of(2020, Month.JANUARY, 13, 9, 0, 0);
        Address address = Address.builder()
            .line1("27")
            .line2("Elm Place")
            .line3("Bangor")
            .postcode("ad21 5dr")
            .build();

        Offence offenceApi = Offence.builder()
            .offenceSummary("On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Person.")
            .offenceTitle("Theft from a shop")
            .act("Contrary to section 1(1) and 7 of the Theft Act 1968.")
            .build();

        CourtCase expected = CourtCase.builder()
            .caseId("1246257")
            .crn("X320741")
            .pnc("D/1234560BC")
            .listNo("2nd")
            .courtCode("SHF")
            .courtRoom("1")
            .sessionStartTime(startTime)
            .probationStatus("Current")
            .breach(Boolean.TRUE)
            .suspendedSentenceOrder(Boolean.FALSE)
            .caseNo("12345")
            .defendantAddress(address)
            .defendantDob(LocalDate.of(1977, Month.DECEMBER, 11))
            .defendantName("Mr Dylan Adam Armstrong")
            .defendantSex("M")
            .nationality1("British")
            .nationality2("Czech")
            .offences(Collections.singletonList(offenceApi))
            .build();

        Optional<CourtCase> optional = restClient.getCourtCase(COURT_CODE, "123456").blockOptional();

        assertThat(optional.get()).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void givenUnknownCaseNo_whenGetCourtCase_thenReturnEmptyOptional() {

        Optional<CourtCase> optional = restClient.getCourtCase(COURT_CODE, NEW_CASE_NO).blockOptional();

        assertThat(optional.isPresent()).isFalse();
    }

    @Test
    public void whenPutCourtCase_thenMakeRestCallToCourtCaseService() {

        restClient.putCourtCase(COURT_CODE, CASE_NO, A_CASE);

        verify(eventBus, timeout(WEB_CLIENT_TIMEOUT_MS)).post(any(CourtCaseSuccessEvent.class));
    }

    @Test
    public void givenUnknownCourt_whenPutCourtCase_thenFailureEvent() {

        String unknownCourtCode = "XXX";
        CourtCase courtCaseApi = CourtCase.builder()
            .caseNo("12345")
            .courtCode(unknownCourtCode)
            .build();

        restClient.putCourtCase(unknownCourtCode, CASE_NO, courtCaseApi);

        verify(eventBus, timeout(WEB_CLIENT_TIMEOUT_MS)).post(any(CourtCaseFailureEvent.class));
    }

    @Test
    public void whenRestClientThrows500OnPut_ThenFailureEvent() {
        restClient.putCourtCase("X500", CASE_NO, A_CASE);
        verify(eventBus, timeout(WEB_CLIENT_TIMEOUT_MS)).post(any(CourtCaseFailureEvent.class));
    }

}
