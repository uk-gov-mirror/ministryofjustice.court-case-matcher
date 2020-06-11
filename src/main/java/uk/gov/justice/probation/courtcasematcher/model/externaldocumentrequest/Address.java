package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Data
@Builder
public class Address {

    private final String line1;
    private final String line2;
    private final String line3;
    private final String line4;
    private final String line5;
    private final String pcode;

}
