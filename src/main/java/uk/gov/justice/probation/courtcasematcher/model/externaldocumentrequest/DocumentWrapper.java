package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class DocumentWrapper
{

    private List<Document> document;
    private String jobNumber;

}
