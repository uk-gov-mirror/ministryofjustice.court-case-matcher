
package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.messaging.SourceFileNameToOuCodeDeserializer;

@Builder
@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonIgnoreProperties(value = {"contentType", "dateOfHearing", "courtHouse", "area"})
public class Info
{

    @JsonDeserialize(using = SourceFileNameToOuCodeDeserializer.class)
    @JacksonXmlProperty(localName = "source_file_name")
    private final String ouCode;

}
