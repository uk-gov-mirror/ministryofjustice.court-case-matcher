package uk.gov.justice.probation.courtcasematcher.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader.MessageID;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Address;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Document;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;

@ExtendWith(SpringExtension.class)
@DisplayName("Message Parser Test")
@Profile("test")
public class MessageParserTest {

    private static final LocalDate HEARING_DATE = LocalDate.of(2020, Month.FEBRUARY, 20);
    private static final LocalTime START_TIME = LocalTime.of(9, 1);
    private static final LocalDateTime SESSION_START_TIME = LocalDateTime.of(HEARING_DATE, START_TIME);

    @Import(TestMessagingConfig.class)
    @Nested
    @DisplayName("GatewayMessage Parser Test")
    class GatewayMessageParser {

        @Autowired
        public MessageParser<MessageType> gatewayMessageParser;

        @Test
        void whenInvalidMessage() throws IOException {
            String path = "src/test/resources/messages/gateway-message-invalid.xml";
            String content = Files.readString(Paths.get(path));

            Throwable thrown = catchThrowable(() -> gatewayMessageParser.parseMessage(content, MessageType.class));

            ConstraintViolationException ex = (ConstraintViolationException) thrown;
            assertThat(ex.getConstraintViolations()).hasSize(2);
            final String docInfoPath = "messageBody.gatewayOperationType.externalDocumentRequest.documentWrapper.document[0].info";
            final String firstSessionPath = "messageBody.gatewayOperationType.externalDocumentRequest.documentWrapper.document[0].data.job.sessions[0]";
            assertThat(ex.getConstraintViolations()).anyMatch(cv -> cv.getMessage().equals("must not be null")
                && cv.getPropertyPath().toString().equals(docInfoPath + ".dateOfHearing"));
            assertThat(ex.getConstraintViolations()).anyMatch(cv -> cv.getMessage().equals("must not be blank")
                && cv.getPropertyPath().toString().equals(firstSessionPath + ".blocks[0].cases[0].caseNo"));
        }

        @Test
        void whenInvalidXmlMessage() {

            Throwable thrown = catchThrowable(() -> gatewayMessageParser.parseMessage("<msg>sss</msg>", MessageType.class));

            ConstraintViolationException ex = (ConstraintViolationException) thrown;
            assertThat(ex.getConstraintViolations()).hasSize(2);
        }

        @DisplayName("Parse a valid message")
        @Test
        void whenValidGatewayMessage_ThenReturnAsObject() throws IOException {
            String path = "src/test/resources/messages/gateway-message-multi-session.xml";
            String content = Files.readString(Paths.get(path));

            MessageType message = gatewayMessageParser.parseMessage(content, MessageType.class);

            MessageHeader expectedHeader = MessageHeader.builder().from("CP_NPS_ML")
                .to("CP_NPS")
                .messageType("externalDocument")
                .timeStamp("2020-05-29T09:16:40.594Z")
                .build();

            assertThat(message.getMessageHeader()).usingRecursiveComparison()
                .ignoringFields("messageID")
                .isEqualTo(expectedHeader);
            assertThat(message.getMessageHeader().getMessageID()).usingRecursiveComparison()
                .isEqualTo(MessageID.builder()
                    .uuid("6be22d98-a8f6-4b2a-b9e7-ca8735037c68")
                    .relatesTo("relatesTo")
                    .build());

            assertThat(message.getMessageBody().getGatewayOperationType().getExternalDocumentRequest().getDocumentWrapper().getDocument()).hasSize(2);

            List<Document> documents = new ArrayList<>(message.getMessageBody().getGatewayOperationType().getExternalDocumentRequest()
                .getDocumentWrapper().getDocument());

            assertThat(documents).hasSize(2);
            Document document = documents.stream()
                .filter(doc -> doc.getInfo().getInfoSourceDetail().getOuCode().equals("B01CX"))
                .findFirst().orElseThrow();

            assertThat(document.getInfo().getInfoSourceDetail().getOuCode()).isEqualTo("B01CX");
            assertThat(document.getData().getJob().getSessions()).hasSize(1);
            checkSession(document.getData().getJob().getSessions().get(0));

            // Check fallback for ou_code when it is not in the session
            Document document2 = documents.stream()
                .filter(doc -> doc.getInfo().getInfoSourceDetail().getOuCode().equals("B01CX"))
                .findFirst().orElseThrow();
            Session session = document2.getData().getJob().getSessions().get(0);
            assertThat(session.getCourtCode()).isEqualTo("B01CX");
        }

    }

    @Import(TestMessagingConfig.class)
    @Nested
    @DisplayName("External Document Parser Test")
    class ExternalDocumentMessageParser {

        @Autowired
        public MessageParser<ExternalDocumentRequest> messageParser;

        @DisplayName("Parse a valid message with multiple sessions")
        @Test
        void whenValidExternalDocumentMessage_ThenReturnAsObject() throws IOException {
            String path = "src/test/resources/messages/external-document-request-multi-session.xml";
            String content = Files.readString(Paths.get(path));

            ExternalDocumentRequest message = messageParser.parseMessage(content, ExternalDocumentRequest.class);

            List<Document> documents = new ArrayList<>(message.getDocumentWrapper().getDocument());

            assertThat(documents).hasSize(2);
            Document document = documents.stream()
                .filter(doc -> doc.getInfo().getInfoSourceDetail().getOuCode().equals("B01CX"))
                .findFirst().orElseThrow();

            assertThat(document.getInfo().getInfoSourceDetail().getOuCode()).isEqualTo("B01CX");
            assertThat(document.getData().getJob().getSessions()).hasSize(1);
            checkSession(document.getData().getJob().getSessions().get(0));

            // Check fallback for ou_code when it is not in the session
            Document document2 = documents.stream()
                .filter(doc -> doc.getInfo().getInfoSourceDetail().getOuCode().equals("B01CX"))
                .findFirst().orElseThrow();
            Session session = document2.getData().getJob().getSessions().get(0);
            assertThat(session.getCourtCode()).isEqualTo("B01CX");
        }

    }

    @TestConfiguration
    public static class TestMessagingConfig {

        @Bean
        public MessageParser<MessageType> gatewayMessageParser() {
            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            XmlMapper mapper = new XmlMapper(xmlModule);
            mapper.registerModule(new JavaTimeModule());
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            return new MessageParser<>(mapper, factory.getValidator());
        }

        @Bean
        public MessageParser<ExternalDocumentRequest> messageParser() {
            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            XmlMapper mapper = new XmlMapper(xmlModule);
            mapper.registerModule(new JavaTimeModule());
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            return new MessageParser<>(mapper, factory.getValidator());
        }

    }

    private static void checkSession(Session session) {
        assertThat(session.getId()).isEqualTo(556805);
        assertThat(session.getDateOfHearing()).isEqualTo(HEARING_DATE);
        assertThat(session.getCourtCode()).isEqualTo("B01CX");
        assertThat(session.getCourtName()).isEqualTo("Camberwell Green");
        assertThat(session.getCourtRoom()).isEqualTo("00");
        assertThat(session.getStart()).isEqualTo(START_TIME);
        assertThat(session.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
        assertThat(session.getEnd()).isEqualTo(LocalTime.of(13, 5));
        assertThat(session.getBlocks()).hasSize(1);
        checkBlock(session.getBlocks().get(0));
    }

    private static void checkBlock(Block block) {
        assertThat(block.getCases()).hasSize(2);
        checkCase(block.getCases().stream().filter(aCase -> aCase.getCaseNo().equals("1600032953")).findFirst().orElseThrow());
    }

    private static void checkCase(Case aCase) {
        // Fields populated from the session
        assertThat(aCase.getDef_age()).isEqualTo("20");
        assertThat(aCase.getId()).isEqualTo(1217464);
        assertThat(aCase.getDef_name()).isEqualTo("Mr. David DLONE");
        assertThat(aCase.getName()).isEqualTo(Name.builder()
                                                .title("Mr.")
                                                .forename1("David")
                                                .surname("DLONE").build());
        assertThat(aCase.getDef_type()).isEqualTo("P");
        assertThat(aCase.getDef_sex()).isEqualTo("N");
        assertThat(aCase.getDef_age()).isEqualTo("20");
        assertThat(aCase.getPnc()).isEqualTo("PNC-ID1");
        assertThat(aCase.getCro()).isEqualTo("11111/79J");
        assertThat(aCase.getDef_addr()).usingRecursiveComparison().isEqualTo(Address.builder()
                                                                    .line1("39 The Street")
                                                                    .line2("Newtown")
                                                                    .pcode("NT4 6YH").build());
        assertThat(aCase.getDef_dob()).isEqualTo(LocalDate.of(2002, Month.FEBRUARY, 2));
        assertThat(aCase.getNationality1()).isEqualTo("Angolan");
        assertThat(aCase.getNationality2()).isEqualTo("Austrian");
        assertThat(aCase.getSeq()).isEqualTo(1);
        assertThat(aCase.getListNo()).isEqualTo("1st");
        assertThat(aCase.getOffences()).hasSize(1);
        checkOffence(aCase.getOffences().get(0));
    }

    private static void checkOffence(Offence offence) {
        assertThat(offence.getSeq()).isEqualTo(1);
        assertThat(offence.getTitle()).isEqualTo("Theft from a shop");
        assertThat(offence.getSum()).isEqualTo("On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Person.");
    }


}
