package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Data
@Builder
public class DocumentWrapper
{
    @NotNull
    private final List<@Valid Document> document;

}
