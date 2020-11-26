package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@Builder
@Data
public class InfoSourceDetail {

    // 160_28072020_2578_B01CX00_ADULT_COURT_LIST_DAILY

    @EqualsAndHashCode.Exclude
    private final long sequence;

    private final String ouCode;

}
