package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Offender {
    private final OtherIds otherIds;
}
