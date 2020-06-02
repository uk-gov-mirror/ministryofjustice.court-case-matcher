
package uk.gov.justice.probation.courtcasematcher.model;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.CSCI_BODY_NS;
import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.CSCI_HDR_NS;
import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.CSC_STATUS_NS;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@JacksonXmlRootElement(localName = "CSCI_Message_Type")
public class MessageType {

    @JacksonXmlProperty(namespace = CSCI_HDR_NS, localName = "MessageHeader")
    private MessageHeader messageHeader;

    @JacksonXmlProperty(namespace = CSCI_BODY_NS, localName = "MessageBody")
    private MessageBodyType messageBody;

    @JacksonXmlProperty(namespace = CSC_STATUS_NS, localName = "MessageStatus")
    private MessageStatus messageStatus;

}
