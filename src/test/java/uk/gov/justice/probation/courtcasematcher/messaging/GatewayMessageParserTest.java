package uk.gov.justice.probation.courtcasematcher.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.SerializationFeature;
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
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.application.CaseMapperReference;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader.MessageID;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Address;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Document;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;

@DisplayName("Gateway Message Parser Test")
public class GatewayMessageParserTest {

    private static final LocalDate HEARING_DATE = LocalDate.of(2020, Month.FEBRUARY, 19);
    private static final LocalTime START_TIME = LocalTime.of(9, 1);
    private static final LocalDateTime SESSION_START_TIME = LocalDateTime.of(HEARING_DATE, START_TIME);
    private static GatewayMessageParser parser;
    private static final CaseMapperReference caseMapperReference = new CaseMapperReference();

    @BeforeAll
    static void beforeAll() {
        caseMapperReference.setDefaultProbationStatus("No record");
        caseMapperReference.setCourtNameToCodes(Map.of("SheffieldMagistratesCourt", "SHF"));
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        final XmlMapper xmlMapper = new XmlMapper(xmlModule);
        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        parser = new GatewayMessageParser(xmlMapper);
    }

    @DisplayName("Parse a valid message")
    @Test
    public void whenValidMessage_ThenReturnAsObject() throws IOException {
        String path = "src/test/resources/messages/gateway-message-full.xml";
        String content = Files.readString(Paths.get(path));

        MessageType message = parser.parseMessage(content);

        MessageHeader expectedHeader = MessageHeader.builder().from("CP_NPS_ML")
            .to("CP_NPS")
            .messageType("externalDocument")
            .timeStamp("2020-05-29T09:16:40.594Z")
            .build();

        assertThat(message.getMessageHeader()).isEqualToIgnoringGivenFields(expectedHeader, "messageID");
        assertThat(message.getMessageHeader().getMessageID()).isEqualToComparingFieldByField(MessageID.builder()
            .uuid("6be22d98-a8f6-4b2a-b9e7-ca8735037c68")
            .relatesTo("")
            .build());

        assertThat(message.getMessageBody().getGatewayOperationType().getExternalDocumentRequest().getDocumentWrapper().getDocument()).hasSize(2);

        Info expectedInfo = Info.builder().area("13")
            .contentType("StandardCourtList")
            .sourceFileName("5_27022020_2992_B13HT00_ADULT_COURT_LIST_DAILY")
            .courtHouse("Sheffield Magistrates Court")
            .dateOfHearing("27/02/2020")
            .build();
        List<Document> documents = new ArrayList<>(message.getMessageBody().getGatewayOperationType().getExternalDocumentRequest()
            .getDocumentWrapper().getDocument());

        assertThat(documents).hasSize(2);
        Document document = documents.stream()
            .filter(doc -> doc.getInfo().getSourceFileName().startsWith("5_"))
            .findFirst().orElseThrow();

        assertThat(document.getInfo()).isEqualToComparingFieldByField(expectedInfo);
        assertThat(document.getData().getJob().getSessions()).hasSize(1);
        checkSession(document.getData().getJob().getSessions().get(0));
    }

    private void checkSession(Session session) {
        assertThat(session.getId()).isEqualTo(556805);
        assertThat(session.getDateOfHearing()).isEqualTo(HEARING_DATE);
        assertThat(session.getLja()).isEqualTo("South West London Magistrates; Court");
        assertThat(session.getCmu()).isEqualTo("Gl Management Unit 1");
        assertThat(session.getPanel()).isEqualTo("Adult Panel");
        assertThat(session.getOuCode()).isEqualTo("B13HT00");
        assertThat(session.getCourtCode()).isEqualTo("SHF");
        assertThat(session.getCourtRoom()).isEqualTo("00");
        assertThat(session.getStart()).isEqualTo(START_TIME);
        assertThat(session.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
        assertThat(session.getEnd()).isEqualTo(LocalTime.of(13, 5));
        assertThat(session.getBlocks()).hasSize(1);
        checkBlock(session.getBlocks().get(0));
    }

    private void checkBlock(Block block) {
        assertThat(block.getId()).isEqualTo(758095);
        assertThat(block.getStart()).isEqualTo(LocalTime.of(8, 45));
        assertThat(block.getEnd()).isEqualTo(LocalTime.of(11, 45));
        assertThat(block.getDesc()).isEqualTo("First Hearings Slot");
        assertThat(block.getCases()).hasSize(9);
        checkCase(block.getCases().stream().filter(aCase -> aCase.getCaseNo().equals("1600032804")).findFirst().orElseThrow());
    }

    private void checkCase(Case aCase) {
        // Fields populated from the session
        assertThat(aCase.getDef_age()).isEqualTo("14");
        assertThat(aCase.getId()).isEqualTo(1215460);
        assertThat(aCase.getH_id()).isEqualTo(1291275);
        assertThat(aCase.getValid()).isEqualTo("Y");
        assertThat(aCase.getType()).isEqualTo("C");
        assertThat(aCase.getProv()).isEqualTo("Y");
        assertThat(aCase.getDef_name()).isEqualTo("Tess TEYOUTHBAILTWO");
        assertThat(aCase.getName()).isEqualTo(Name.builder()
                                                .title("Ms")
                                                .forename1("Tess")
                                                .forename2("name2")
                                                .forename3("name3")
                                                .surname("TEYOUTHBAILTWO").build());
        assertThat(aCase.getDef_type()).isEqualTo("P");
        assertThat(aCase.getDef_sex()).isEqualTo("F");
        assertThat(aCase.getDef_age()).isEqualTo("14");
        assertThat(aCase.getPnc()).isEqualTo("PNC-ID");
        assertThat(aCase.getCro()).isEqualTo("12345/678E");
        assertThat(aCase.getDef_addr()).isEqualToComparingFieldByField(Address.builder()
                                                                    .line1("39 The Stree")
                                                                    .line2("Newtown")
                                                                    .pcode("NT4 6YH").build());
        assertThat(aCase.getDef_dob()).isEqualTo(LocalDate.of(2002, Month.FEBRUARY, 2));
        assertThat(aCase.getInf()).isEqualTo("POL01");
        assertThat(aCase.getSeq()).isEqualTo(9);
        assertThat(aCase.getListNo()).isEqualTo("2nd");
        assertThat(aCase.getPg_type()).isEqualTo("P");
        assertThat(aCase.getPg_name()).isEqualTo("Tog TEYOUTHBAILTWOGUARDIAN");
        assertThat(aCase.getPg_addr()).isEqualToComparingFieldByField(Address.builder()
                                                                    .line1("39 The Street")
                                                                    .line2("Newtown")
                                                                    .pcode("NT45 5YJ").build());
        assertThat(aCase.getOffences()).hasSize(1);
        checkOffence(aCase.getOffences().get(0));
    }

    private void checkOffence(Offence offence) {
        assertThat(offence.getSeq()).isEqualTo(1);
        assertThat(offence.getCo_id()).isEqualTo(1182407);
        assertThat(offence.getCode()).isEqualTo("TA02003");
        assertThat(offence.getTitle()).isEqualTo("Printed in the U.K. a tobacco advertisement");
        assertThat(offence.getSum()).isEqualTo("Blah");
        assertThat(offence.getPlea()).isEqualTo("NG");
        assertThat(offence.getPleaDate()).isEqualTo(LocalDate.of(2016, Month.SEPTEMBER, 28));
        assertThat(offence.getConvdate()).isEqualTo(LocalDate.of(2016, Month.SEPTEMBER, 28));
    }

}
