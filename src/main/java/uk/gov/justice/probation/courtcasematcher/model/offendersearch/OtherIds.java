package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class OtherIds {
    private final String crn;
    private final String cro;
    private final String pnc;
}
