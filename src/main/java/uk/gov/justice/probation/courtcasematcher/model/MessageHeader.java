
package uk.gov.justice.probation.courtcasematcher.model;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.CSCI_HDR_NS;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class MessageHeader {

    @JacksonXmlProperty(namespace = CSCI_HDR_NS, localName = "MessageID")
    private MessageID messageID;

    @JacksonXmlProperty(namespace = CSCI_HDR_NS, localName = "TimeStamp")
    private String timeStamp;

    @JacksonXmlProperty(namespace = CSCI_HDR_NS, localName = "MessageType")
    private String messageType;

    @JacksonXmlProperty(namespace = CSCI_HDR_NS, localName = "From")
    private String from;
    @JacksonXmlProperty(namespace = CSCI_HDR_NS, localName = "To")
    private String to;

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class MessageID {

        @JacksonXmlProperty(localName = "UUID")
        private String uuid;
        @JacksonXmlProperty(localName = "RelatesTo")
        private String relatesTo;
    }

}
