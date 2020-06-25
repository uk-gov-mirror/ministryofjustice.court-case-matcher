package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.EXT_DOC_NS;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
public class Case {

    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "c_id")
    private final Long id;
    private final Long h_id;
    private final String valid;
    private final String prov;

    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "caseno")
    private final String caseNo;
    private final String inf;
    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "cseq")
    private final Integer seq;
    private final String type;

    @JacksonXmlProperty(localName = "def_name_elements")
    private final Name name;

    private final String def_name;
    private final String def_type;
    private final String def_sex;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", lenient = OptBoolean.TRUE)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private final LocalDate def_dob;
    @JacksonXmlProperty(localName = "def_addr")
    private final Address def_addr;
    private final String def_age;

    @JacksonXmlProperty(localName = "cro_number")
    private final String cro;

    @JacksonXmlProperty(localName = "pnc_id")
    private final String pnc;

    private final String pg_type;
    private final String pg_name;
    private final Address pg_addr;

    @JacksonXmlProperty(localName = "listno")
    private final String listNo;

    private final String nationality1;
    private final String nationality2;

    @JacksonXmlElementWrapper
    private final List<Offence> offences;

    @JsonBackReference
    private final Block block;

}
