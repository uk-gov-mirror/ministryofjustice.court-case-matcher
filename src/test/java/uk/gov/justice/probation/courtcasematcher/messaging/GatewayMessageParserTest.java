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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.probation.courtcasematcher.application.CaseMapperReference;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader.MessageID;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Address;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Document;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;

@ExtendWith(SpringExtension.class)
@DisplayName("Gateway Message Parser Test")
public class GatewayMessageParserTest {

    private static final LocalDate HEARING_DATE = LocalDate.of(2020, Month.FEBRUARY, 20);
    private static final LocalTime START_TIME = LocalTime.of(9, 1);
    private static final LocalDateTime SESSION_START_TIME = LocalDateTime.of(HEARING_DATE, START_TIME);
    private static final CaseMapperReference caseMapperReference = new CaseMapperReference();

    @Autowired
    private GatewayMessageParser gatewayMessageParser;

    @BeforeAll
    static void beforeAll() {
        caseMapperReference.setDefaultProbationStatus("No record");
    }

    @Test
    void whenInvalidMessage() throws IOException {
        String path = "src/test/resources/messages/gateway-message-invalid.xml";
        String content = Files.readString(Paths.get(path));

        Throwable thrown = catchThrowable(() -> gatewayMessageParser.parseMessage(content));

        ConstraintViolationException ex = (ConstraintViolationException) thrown;
        assertThat(ex.getConstraintViolations()).hasSize(4);
        final String firstSessionPath = "messageBody.gatewayOperationType.externalDocumentRequest.documentWrapper.document[0].data.job.sessions[0]";
        assertThat(ex.getConstraintViolations()).anyMatch(cv -> cv.getMessage().equals("must not be null")
            && cv.getPropertyPath().toString().equals(firstSessionPath + ".courtRoom"));
        assertThat(ex.getConstraintViolations()).anyMatch(cv -> cv.getMessage().equals("must not be null")
            && cv.getPropertyPath().toString().equals(firstSessionPath + ".id"));
        assertThat(ex.getConstraintViolations()).anyMatch(cv -> cv.getMessage().equals("must not be blank")
            && cv.getPropertyPath().toString().equals(firstSessionPath + ".blocks[0].cases[0].caseNo"));
        assertThat(ex.getConstraintViolations()).anyMatch(cv -> cv.getMessage().equals("must not be blank")
            && cv.getPropertyPath().toString().equals(firstSessionPath + ".blocks[0].cases[0].offences[0].title"));
    }

    @DisplayName("Parse a valid message")
    @Test
    void whenValidMessage_ThenReturnAsObject() throws IOException {
        String path = "src/test/resources/messages/gateway-message-multi-session.xml";
        String content = Files.readString(Paths.get(path));

        MessageType message = gatewayMessageParser.parseMessage(content);

        MessageHeader expectedHeader = MessageHeader.builder().from("CP_NPS_ML")
            .to("CP_NPS")
            .messageType("externalDocument")
            .timeStamp("2020-05-29T09:16:40.594Z")
            .build();

        assertThat(message.getMessageHeader()).isEqualToIgnoringGivenFields(expectedHeader, "messageID");
        assertThat(message.getMessageHeader().getMessageID()).isEqualToComparingFieldByField(MessageID.builder()
            .uuid("6be22d98-a8f6-4b2a-b9e7-ca8735037c68")
            .relatesTo("relatesTo")
            .build());

        assertThat(message.getMessageBody().getGatewayOperationType().getExternalDocumentRequest().getDocumentWrapper().getDocument()).hasSize(2);

        List<Document> documents = new ArrayList<>(message.getMessageBody().getGatewayOperationType().getExternalDocumentRequest()
            .getDocumentWrapper().getDocument());

        assertThat(documents).hasSize(2);
        Document document = documents.stream()
            .filter(doc -> doc.getInfo().getInfoSourceDetail().getOuCode().equals("B01CX00"))
            .findFirst().orElseThrow();

        assertThat(document.getInfo().getInfoSourceDetail().getOuCode()).isEqualTo("B01CX00");
        assertThat(document.getData().getJob().getSessions()).hasSize(1);
        checkSession(document.getData().getJob().getSessions().get(0));
    }

    private void checkSession(Session session) {
        assertThat(session.getId()).isEqualTo(556805);
        assertThat(session.getDateOfHearing()).isEqualTo(HEARING_DATE);
        assertThat(session.getCourtCode()).isEqualTo("B01CX00");
        assertThat(session.getCourtName()).isEqualTo("Camberwell Green");
        assertThat(session.getCourtRoom()).isEqualTo("00");
        assertThat(session.getStart()).isEqualTo(START_TIME);
        assertThat(session.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
        assertThat(session.getEnd()).isEqualTo(LocalTime.of(13, 5));
        assertThat(session.getBlocks()).hasSize(1);
        checkBlock(session.getBlocks().get(0));
    }

    private void checkBlock(Block block) {
        assertThat(block.getCases()).hasSize(2);
        checkCase(block.getCases().stream().filter(aCase -> aCase.getCaseNo().equals("1600032952")).findFirst().orElseThrow());
    }

    private void checkCase(Case aCase) {
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
        assertThat(aCase.getDef_addr()).isEqualToComparingFieldByField(Address.builder()
                                                                    .line1("39 The Street")
                                                                    .line2("Newtown")
                                                                    .pcode("NT4 6YH").build());
        assertThat(aCase.getDef_dob()).isEqualTo(LocalDate.of(2002, Month.FEBRUARY, 2));
        assertThat(aCase.getSeq()).isEqualTo(1);
        assertThat(aCase.getListNo()).isEqualTo("1st");
        assertThat(aCase.getOffences()).hasSize(1);
        checkOffence(aCase.getOffences().get(0));
    }

    private void checkOffence(Offence offence) {
        assertThat(offence.getSeq()).isEqualTo(1);
        assertThat(offence.getTitle()).isEqualTo("Theft from a shop");
        assertThat(offence.getSum()).isEqualTo("On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Person.");
    }

    @TestConfiguration
    public static class TestMessagingConfig {

        @Bean(name = "testGatewayMessageParser")
        public GatewayMessageParser testGatewayMessageParser() {
            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            XmlMapper mapper = new XmlMapper(xmlModule);
            mapper.registerModule(new JavaTimeModule());
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            return new GatewayMessageParser(mapper, factory.getValidator());
        }
    }
}
