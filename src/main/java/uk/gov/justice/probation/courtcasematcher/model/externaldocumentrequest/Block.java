package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(value = {"bend", "desc", "sb_id", "bstart", "bend"})
public class Block {

    @Getter(AccessLevel.NONE)
    @JsonManagedReference
    @JacksonXmlElementWrapper
    @ToString.Exclude
    private final List<@Valid Case> cases;

    @JsonBackReference
    private final Session session;

    public List<Case> getCases() {
        return cases != null ? cases : Collections.emptyList();
    }

}
