package uk.gov.justice.probation.courtcasematcher.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatcherServiceTest {
    private static final String COURT_CODE = "SHF";
    private static final String CASE_NO = "1600032952";
    private static final long REST_CLIENT_WAIT_MS = 2000;

    private final Case incomingCase = Case.builder()
            .caseNo(CASE_NO)
            .block(Block.builder()
                    .session(Session.builder()
                            .courtCode(COURT_CODE)
                            .build())
                    .build())
            .build();
    private final CourtCase courtCase = CourtCase.builder()
            .caseNo(CASE_NO)
            .courtCode(COURT_CODE)
            .build();

    @Mock
    private CourtCaseRestClient courtCaseRestClient;
    @Mock
    private CaseMapper caseMapper;

    private MatcherService matcherService;

    @BeforeEach
    public void setUp() {
        matcherService = new MatcherService(courtCaseRestClient, caseMapper);
    }

    @Test
    public void givenIncomingCaseMatchesExisting_whenMatchCalled_thenMergeAndStore() {
        Disposable disposable = Mockito.mock(Disposable.class);

        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.just(courtCase));
        when(caseMapper.merge(any(Case.class), eq(courtCase))).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase))).thenReturn(disposable);

        matcherService.match(incomingCase);

        verify(caseMapper).merge(any(Case.class), eq(courtCase));
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase));
    }


    @Test
    void givenIncomingCaseDoesNotMatchExisting_whenMatchCalled_thenCreateNewRecord(){
        Disposable disposable = Mockito.mock(Disposable.class);

        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(caseMapper.newFromCase(any(Case.class))).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase))).thenReturn(disposable);

        matcherService.match(incomingCase);

        verify(caseMapper).newFromCase(any(Case.class));
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase));
    }
}