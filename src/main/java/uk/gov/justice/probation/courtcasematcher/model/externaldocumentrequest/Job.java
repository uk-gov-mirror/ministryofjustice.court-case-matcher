package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Job {

    @JacksonXmlProperty(localName = "printdate")
    private String printDate;
    private String username;
    private String late;
    private String adbox;
    private String means;

    @JacksonXmlElementWrapper
    private List<Session> sessions;
}
