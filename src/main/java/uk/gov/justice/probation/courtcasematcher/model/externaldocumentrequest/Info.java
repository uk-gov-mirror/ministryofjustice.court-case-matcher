
package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.messaging.SourceFileNameDeserializer;

@Builder
@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonIgnoreProperties(value = {"contentType"})
public class Info {

    @Exclude
    @JsonDeserialize(using = SourceFileNameDeserializer.class)
    @JacksonXmlProperty(localName = "source_file_name")
    private final InfoSourceDetail infoSourceDetail;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", lenient = OptBoolean.TRUE)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private final LocalDate dateOfHearing;

    private final String area;

    private final String courtHouse;

}
