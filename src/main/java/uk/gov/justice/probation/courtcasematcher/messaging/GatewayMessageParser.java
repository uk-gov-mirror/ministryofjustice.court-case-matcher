package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;

@Service
@Slf4j
public class GatewayMessageParser {

    private final XmlMapper xmlMapper;

    public static final String EXT_DOC_NS = "http://www.justice.gov.uk/magistrates/external/ExternalDocumentRequest";
    public static final String CSCI_HDR_NS = "http://www.justice.gov.uk/magistrates/generic/CSCI_Header";
    public static final String CSCI_BODY_NS = "http://www.justice.gov.uk/magistrates/cp/CSCI_Body";
    public static final String CSC_STATUS_NS = "http://www.justice.gov.uk/magistrates/generic/CSCI_Status";
    public static final String GW_MSG_SCHEMA = "http://www.justice.gov.uk/magistrates/cp/GatewayMessageSchema";

    public GatewayMessageParser(@Qualifier("gatewayMessageXmlMapper") XmlMapper xmlMapper) {
        super();
        this.xmlMapper = xmlMapper;
    }

    public MessageType parseMessage (String xml) throws JsonProcessingException {
        return xmlMapper.readValue(xml, MessageType.class);
    }

}
