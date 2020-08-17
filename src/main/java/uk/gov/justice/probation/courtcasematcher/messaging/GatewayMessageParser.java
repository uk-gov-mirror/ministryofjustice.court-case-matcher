package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;

@Service
@Slf4j
public class GatewayMessageParser {

    public static final String EXT_DOC_NS = "http://www.justice.gov.uk/magistrates/external/ExternalDocumentRequest";
    public static final String CSCI_HDR_NS = "http://www.justice.gov.uk/magistrates/generic/CSCI_Header";
    public static final String CSCI_BODY_NS = "http://www.justice.gov.uk/magistrates/cp/CSCI_Body";
    public static final String CSC_STATUS_NS = "http://www.justice.gov.uk/magistrates/generic/CSCI_Status";
    public static final String GW_MSG_SCHEMA = "http://www.justice.gov.uk/magistrates/cp/GatewayMessageSchema";

    private final Validator validator;
    private final XmlMapper xmlMapper;

    public GatewayMessageParser(@Qualifier("gatewayMessageXmlMapper") XmlMapper xmlMapper, @Autowired Validator validator) {
        super();
        this.xmlMapper = xmlMapper;
        this.validator = validator;
        this.xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public MessageType parseMessage (String xml) throws IOException {
        MessageType messageType = xmlMapper.readValue(xml, MessageType.class);
        validate(messageType);
        return messageType;
    }

    private void validate(MessageType messageType) {
        Set<ConstraintViolation<Object>> errors = validator.validate(messageType);
        if (!errors.isEmpty()) {
            throw new ConstraintViolationException(errors);
        }
        if (messageType.getMessageBody() == null) {
            throw new ConstraintViolationException("null message body", null);
        }

    }

}
