package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import lombok.*;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Match {
    private final Offender offender;
}
