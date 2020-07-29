package uk.gov.justice.probation.courtcasematcher.restclient.exception;

public class CourtNotFoundException extends Exception {
    public CourtNotFoundException(String courtCode) {
        super(String.format("Court not found for code '%s", courtCode));
    }
}
