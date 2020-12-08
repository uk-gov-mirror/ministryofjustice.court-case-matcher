package uk.gov.justice.probation.courtcasematcher.event;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;
import uk.gov.justice.probation.courtcasematcher.service.SearchResult;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import java.util.Collections;

/**
 * We intend to replace EventBus with an external message queue.
 */
@Component
@Slf4j
public class EventListener {

    private final CourtCaseService courtCaseService;

    private final MatcherService matcherService;

    private final TelemetryService telemetryService;

    @Autowired
    public EventListener(EventBus eventBus,
                         MatcherService matcherService,
                         CourtCaseService courtCaseService,
                         TelemetryService telemetryService) {
        super();
        this.matcherService = matcherService;
        this.courtCaseService = courtCaseService;
        this.telemetryService = telemetryService;
        eventBus.register(this);
    }

    @AllowConcurrentEvents
    @Subscribe
    public void courtCaseEvent(CourtCaseFailureEvent courtCaseEvent) {
        log.error("Message processing failed. Error: {} ", courtCaseEvent.getFailureMessage(), courtCaseEvent.getThrowable());
        if (!CollectionUtils.isEmpty(courtCaseEvent.getViolations())) {
            courtCaseEvent.getViolations().forEach(
                cv -> log.error("Validation failed : {} at {} ", cv.getMessage(), cv.getPropertyPath().toString())
            );
        }
    }

    @AllowConcurrentEvents
    @Subscribe
    public void courtCaseEvent(CourtCaseSuccessEvent courtCaseEvent) {
        String caseNo = courtCaseEvent.getCourtCase().getCaseNo();
        String court = courtCaseEvent.getCourtCase().getCourtCode();
        log.info("EventBus success event for posting case {} for court {}. ", caseNo, court);
    }

    @AllowConcurrentEvents
    @Subscribe
    public void courtCaseUpdateEvent(CourtCaseUpdateEvent courtCaseEvent) {
        CourtCase courtCase = courtCaseEvent.getCourtCase();
        log.info("Upsert case no {} for court {}", courtCase.getCaseNo(), courtCase.getCourtCode());
        courtCaseService.saveCourtCase(courtCaseEvent.getCourtCase());
    }

    @AllowConcurrentEvents
    @Subscribe
    public void courtCaseMatchEvent(CourtCaseMatchEvent courtCaseEvent) {

        CourtCase courtCase = courtCaseEvent.getCourtCase();
        log.info("Matching offender and saving case no {} for court {}, defendant name {}, pnc {}",
            courtCase.getCaseNo(), courtCase.getCourtCode(), courtCase.getDefendantName(), courtCase.getPnc());

        matcherService.getSearchResponse(courtCase)
            .doOnSuccess(searchResult -> telemetryService.trackOffenderMatchEvent(courtCase, searchResult.getSearchResponse()))
            .doOnError(throwable -> telemetryService.trackOffenderMatchFailureEvent(courtCase))
            .onErrorResume(throwable -> Mono.just(SearchResult.builder()
                    .searchResponse(
                        SearchResponse.builder()
                        .matchedBy(OffenderSearchMatchType.NOTHING)
                        .matches(Collections.emptyList())
                        .build())
                    .build()))
            .subscribe(searchResult -> courtCaseService.createCase(courtCase, searchResult))
            ;

    }

}
