package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.EXT_DOC_NS;

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
public class Case {

    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "c_id")
    private Long id;
    private Long h_id;
    private String valid;
    private String prov;
    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "caseno")
    private String caseNo;
    private String inf;
    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "cseq")
    private Integer seq;
    private String type;

    private String def_name;
    private String def_type;
    private String def_sex;
    private String def_dob;
    @JacksonXmlProperty(localName = "def_addr")
    private Address def_addr;
    private String def_age;

    private String pg_type;
    private String pg_name;
    private Address pg_addr;

    @JacksonXmlProperty(localName = "listno")
    private String listNo;

    @JacksonXmlElementWrapper
    private List<Offence> offences;

}
