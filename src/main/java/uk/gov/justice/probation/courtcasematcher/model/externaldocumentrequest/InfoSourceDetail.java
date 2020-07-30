package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class InfoSourceDetail {

    // 160_28072020_2578_B01CX00_ADULT_COURT_LIST_DAILY

    private final long sequence;

    private final String ouCode;

}
