package uk.gov.justice.probation.courtcasematcher.restclient;

import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Search;

import java.time.LocalDate;
import java.util.Optional;

public class OffenderSearchRestClient {
    public Optional<Search> match(String def_name, LocalDate def_dob){
        return Optional.empty();
    }
}
