
package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.probation.courtcasematcher.messaging.DocumentInfoDeserializer;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Builder
@Data
public class Document
{
    @Valid
    @JsonDeserialize(using = DocumentInfoDeserializer.class)
    private final Info info;

    @ToString.Exclude
    @JsonManagedReference
    @NotNull
    @Valid
    private final DataJob data;

}
