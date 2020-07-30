package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
@JsonIgnoreProperties(value = {"printdate", "username", "late", "adbox", "means"})
public class Job {

    @ToString.Exclude
    @JsonManagedReference
    @JacksonXmlElementWrapper
    @NotEmpty
    private final List<@Valid Session> sessions;

    @JsonBackReference
    private final DataJob dataJob;

}
