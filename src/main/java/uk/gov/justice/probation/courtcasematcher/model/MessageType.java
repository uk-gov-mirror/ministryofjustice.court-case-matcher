
package uk.gov.justice.probation.courtcasematcher.model;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.CSCI_BODY_NS;
import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.CSCI_HDR_NS;
import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.CSC_STATUS_NS;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
@JacksonXmlRootElement(localName = "CSCI_Message_Type")
public class MessageType {

    @NotNull
    @JacksonXmlProperty(namespace = CSCI_HDR_NS, localName = "MessageHeader")
    @Valid
    private final MessageHeader messageHeader;

    @JacksonXmlProperty(namespace = CSCI_BODY_NS, localName = "MessageBody")
    @NotNull
    @Valid
    private final MessageBodyType messageBody;

    @JacksonXmlProperty(namespace = CSC_STATUS_NS, localName = "MessageStatus")
    private final MessageStatus messageStatus;

}
