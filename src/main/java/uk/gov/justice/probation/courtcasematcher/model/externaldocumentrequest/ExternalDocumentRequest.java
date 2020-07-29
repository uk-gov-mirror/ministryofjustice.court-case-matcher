
package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.EXT_DOC_NS;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Data
@Builder
public class ExternalDocumentRequest
{
    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "documents")
    @NotNull
    @Valid
    private final DocumentWrapper documentWrapper;

}
