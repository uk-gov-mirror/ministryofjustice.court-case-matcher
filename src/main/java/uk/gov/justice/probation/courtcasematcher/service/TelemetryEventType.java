package uk.gov.justice.probation.courtcasematcher.service;

public enum TelemetryEventType {
    OFFENDER_EXACT_MATCH("PiCOffenderExactMatch"),
    OFFENDER_PARTIAL_MATCH("PiCOffenderPartialMatch"),
    OFFENDER_NO_MATCH("PiCOffenderNoMatch"),
    COURT_LIST_MESSAGE_RECEIVED("PiCCourtListMessageReceived"),
    COURT_LIST_RECEIVED("PiCCourtListReceived"),
    COURT_CASE_RECEIVED("PiCCourtCaseReceived")
    ;

    final String eventName;

    TelemetryEventType(String eventName) {
        this.eventName = eventName;
    }
}
