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
public class Block {

    @JacksonXmlProperty(localName = "sb_id")
    private Long id;
    @JacksonXmlProperty(localName = "bstart")
    private String start;
    @JacksonXmlProperty(localName = "bend")
    private String end;
    private String desc;

    @JacksonXmlElementWrapper
    private List<Case> cases;
}
