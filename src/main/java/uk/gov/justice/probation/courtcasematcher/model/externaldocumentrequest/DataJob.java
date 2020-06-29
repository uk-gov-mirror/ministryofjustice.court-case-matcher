
package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
public class DataJob
{
    @NotNull
    @Valid
    private final Job job;
}
