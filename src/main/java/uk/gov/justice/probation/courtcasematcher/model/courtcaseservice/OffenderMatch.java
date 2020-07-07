package uk.gov.justice.probation.courtcasematcher.model.courtcaseservice;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class OffenderMatch {
    @NotNull
    @Valid
    private final MatchIdentifiers matchIdentifiers;
    @NotNull
    private final MatchType matchType;
    @NotNull
    private final Boolean confirmed;
}
