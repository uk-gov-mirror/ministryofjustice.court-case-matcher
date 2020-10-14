package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import java.util.Collections;
import java.util.List;
import javax.validation.Valid;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
public class Job {

    @Getter(AccessLevel.NONE)
    @ToString.Exclude
    @JsonManagedReference
    @JacksonXmlElementWrapper
    private final List<@Valid Session> sessions;

    @JsonBackReference
    private final DataJob dataJob;

    public List<Session> getSessions() {
        return sessions != null ? sessions : Collections.emptyList();
    }

}
