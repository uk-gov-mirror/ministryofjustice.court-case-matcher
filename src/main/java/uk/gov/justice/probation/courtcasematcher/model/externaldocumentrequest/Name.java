package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
public class Name {

    private final String title;
    private final String forename1;
    private final String forename2;
    private final String forename3;
    private final String surname;

}
