package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import java.util.List;
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

    private final List<Document> document;
    private final String jobNumber;

}
