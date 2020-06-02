
package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.EXT_DOC_NS;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ExternalDocumentRequest
{
    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "documents")
    private DocumentWrapper documentWrapper;

}
