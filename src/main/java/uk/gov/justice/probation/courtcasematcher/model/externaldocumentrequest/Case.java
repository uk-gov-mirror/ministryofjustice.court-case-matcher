package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
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
public class Case {

    @JacksonXmlProperty(localName = "c_id")
    private final Long id;

    @NotBlank
    @JacksonXmlProperty(localName = "caseno")
    private final String caseNo;
    @PositiveOrZero
    @JacksonXmlProperty(localName = "cseq")
    private final Integer seq;

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

    @JacksonXmlProperty(localName = "listno")
    private final String listNo;

    @JacksonXmlProperty(localName = "nationality_1")
    private final String nationality1;

    @JacksonXmlProperty(localName = "nationality_2")
    private final String nationality2;

    @ToString.Exclude
    @JacksonXmlElementWrapper
    private final List<@Valid Offence> offences;

    @JsonBackReference
    private final Block block;

}
