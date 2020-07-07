package uk.gov.justice.probation.courtcasematcher.model.courtcaseservice;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatchIdentifiers {
    @NotNull
    private final String crn;
    private final String pnc;
    private final String cro;
}
