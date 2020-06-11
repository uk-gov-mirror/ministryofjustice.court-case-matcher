package uk.gov.justice.probation.courtcasematcher.event;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * We intend to replace EventBus with an external message queue.
 */
@Component
@Slf4j
public class EventListener {

    private final AtomicLong successCount = new AtomicLong(0);

    private final AtomicLong failureCount = new AtomicLong(0);

    public EventListener(EventBus eventBus) {
        super();
        eventBus.register(this);
    }

    @AllowConcurrentEvents
    @Subscribe
    public void courtCaseEvent(CourtCaseFailureEvent courtCaseEvent) {
        log.error("Message processing failed. Current error count: {}. Error: {} ",
            failureCount.incrementAndGet(), courtCaseEvent.getFailureMessage());
    }

    @AllowConcurrentEvents
    @Subscribe
    public void courtCaseEvent(CourtCaseSuccessEvent courtCaseEvent) {
        String caseNo = courtCaseEvent.getCourtCaseApi().getCaseNo();
        String court = courtCaseEvent.getCourtCaseApi().getCourtCode();
        log.info("EventBus success event for posting case {} for court {}. Total count of successful messages {} ",
            caseNo, court, successCount.incrementAndGet());
    }

    public long getSuccessCount() {
        return successCount.get();
    }

    public long getFailureCount() {
        return failureCount.get();
    }
}
