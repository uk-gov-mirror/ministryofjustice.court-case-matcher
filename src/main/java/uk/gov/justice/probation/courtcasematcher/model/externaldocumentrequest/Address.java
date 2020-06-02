package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Address {

    private String line1;
    private String line2;
    private String line3;
    private String town;
    private String pcode;
}
