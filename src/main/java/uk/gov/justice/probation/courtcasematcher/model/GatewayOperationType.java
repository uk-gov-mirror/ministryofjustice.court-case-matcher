
package uk.gov.justice.probation.courtcasematcher.model;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.EXT_DOC_NS;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(value = { "acknowledgement" })
public class GatewayOperationType {

    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "ExternalDocumentRequest")
    private ExternalDocumentRequest externalDocumentRequest;

}
