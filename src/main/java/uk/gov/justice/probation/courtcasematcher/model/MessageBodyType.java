
package uk.gov.justice.probation.courtcasematcher.model;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.GW_MSG_SCHEMA;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MessageBodyType {

    @JacksonXmlProperty(namespace = GW_MSG_SCHEMA, localName = "GatewayOperationType")
    private GatewayOperationType gatewayOperationType;

}
