package uk.gov.justice.probation.courtcasematcher.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.Valid;
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
public class GroupedOffenderMatches {
    @NotNull
    @JsonProperty("matches")
    @Valid
    private final List<OffenderMatch> matches;
}
