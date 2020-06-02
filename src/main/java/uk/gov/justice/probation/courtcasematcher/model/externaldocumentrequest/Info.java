
package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Info
{

    @JacksonXmlProperty(localName = "contentType")
    private String contentType;
    private String dateOfHearing;
    private String courtHouse;
    private String area;
    @JacksonXmlProperty(localName = "source_file_name")
    private String sourceFileName;

}
