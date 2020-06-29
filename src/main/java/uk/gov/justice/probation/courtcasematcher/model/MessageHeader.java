
package uk.gov.justice.probation.courtcasematcher.model;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.CSCI_HDR_NS;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Getter
public class MessageHeader {

    @NotNull
    @JacksonXmlProperty(namespace = CSCI_HDR_NS, localName = "MessageID")
    private final MessageID messageID;

    @NotNull
    @JacksonXmlProperty(namespace = CSCI_HDR_NS, localName = "TimeStamp")
    private final String timeStamp;

    @NotNull
    @JacksonXmlProperty(namespace = CSCI_HDR_NS, localName = "MessageType")
    private final String messageType;

    @NotNull
    @JacksonXmlProperty(namespace = CSCI_HDR_NS, localName = "From")
    private final String from;
    @NotNull
    @JacksonXmlProperty(namespace = CSCI_HDR_NS, localName = "To")
    private final String to;

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
    @Getter
    public static class MessageID {

        @NotBlank
        @JacksonXmlProperty(localName = "UUID")
        private final String uuid;
        @JacksonXmlProperty(localName = "RelatesTo")
        private final String relatesTo;
    }

}
