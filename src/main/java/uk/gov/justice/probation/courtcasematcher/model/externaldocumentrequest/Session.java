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
public class Session {

    @JacksonXmlProperty(localName = "s_id")
    private Long id;

    @JacksonXmlProperty(localName = "doh")
    private String dateOfHearing;
    private String lja;
    private String cmu;
    private String panel;
    private String court;
    private String room;

    @JacksonXmlProperty(localName = "sstart")
    private String start;
    @JacksonXmlProperty(localName = "send")
    private String end;

    @JacksonXmlElementWrapper
    private List<Block> blocks;
}
