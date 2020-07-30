
package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Builder
@Data
@JsonIgnoreProperties(value = "jsonData")
public class Document
{
    @Valid
    private final Info info;

    @ToString.Exclude
    @JsonManagedReference
    @NotNull
    @Valid
    private final DataJob data;

}
