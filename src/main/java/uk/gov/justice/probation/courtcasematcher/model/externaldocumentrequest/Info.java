
package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Info
{

    @JacksonXmlProperty(localName = "contentType")
    private final String contentType;
    private final String dateOfHearing;
    private final String courtHouse;
    private final String area;
    @JacksonXmlProperty(localName = "source_file_name")
    private final String sourceFileName;

}
