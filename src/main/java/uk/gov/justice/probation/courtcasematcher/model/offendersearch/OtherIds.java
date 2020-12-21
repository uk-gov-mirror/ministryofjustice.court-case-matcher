package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class OtherIds {
    private final String crn;
    private final String croNumber;
    private final String pncNumber;
}
