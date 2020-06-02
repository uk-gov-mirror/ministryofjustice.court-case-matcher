package uk.gov.justice.probation.courtcasematcher.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Address;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Document;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader.MessageID;

@DisplayName("Gateway Message Parser Test")
public class GatewayMessageParserTest {

    private static GatewayMessageParser parser;

    @BeforeAll
    static void beforeAll() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        parser = new GatewayMessageParser(new XmlMapper(xmlModule));
    }

    @DisplayName("Parse a valid message")
    @Test
    public void whenValidMessage_ThenReturnAsObject() throws IOException {
        String path = "src/test/resources/messages/externDoc.xml";
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
            .courtHouse("West London")
            .dateOfHearing("27/02/2020")
            .build();
        Document document = message.getMessageBody().getGatewayOperationType().getExternalDocumentRequest().getDocumentWrapper().getDocument().stream().filter(document1 -> document1.getInfo().getSourceFileName().startsWith("5_")).findFirst().orElseThrow();

        assertThat(document.getInfo()).isEqualToComparingFieldByField(expectedInfo);

        assertThat(document.getData().getJob().getSessions()).hasSize(1);
        checkSession(document.getData().getJob().getSessions().get(0));
    }

    private void checkSession(Session session) {
        assertThat(session.getId()).isEqualTo(556805);
        assertThat(session.getDateOfHearing()).isEqualTo("19/02/2020");
        assertThat(session.getLja()).isEqualTo("South West London Magistrates; Court");
        assertThat(session.getCmu()).isEqualTo("Gl Management Unit 1");
        assertThat(session.getPanel()).isEqualTo("Adult Panel");
        assertThat(session.getCourt()).isEqualTo("West London");
        assertThat(session.getRoom()).isEqualTo("00");
        assertThat(session.getStart()).isEqualTo("09:00");
        assertThat(session.getEnd()).isEqualTo("12:00");
        assertThat(session.getBlocks()).hasSize(1);
        checkBlock(session.getBlocks().get(0));
    }

    private void checkBlock(Block block) {
        assertThat(block.getId()).isEqualTo(758095);
        assertThat(block.getStart()).isEqualTo("09:00");
        assertThat(block.getEnd()).isEqualTo("12:00");
        assertThat(block.getDesc()).isEqualTo("First Hearings Slot");
        assertThat(block.getCases()).hasSize(9);
        checkCase(block.getCases().stream().filter(aCase -> aCase.getCaseNo().equals("1600032804")).findFirst().orElseThrow());
    }

    private void checkCase(Case aCase) {
        assertThat(aCase.getDef_age()).isEqualTo("14");
        assertThat(aCase.getId()).isEqualTo(1215460);
        assertThat(aCase.getH_id()).isEqualTo(1291275);
        assertThat(aCase.getValid()).isEqualTo("Y");
        assertThat(aCase.getType()).isEqualTo("C");
        assertThat(aCase.getProv()).isEqualTo("Y");
        assertThat(aCase.getDef_name()).isEqualTo("Tess TEYOUTHBAILTWO");
        assertThat(aCase.getDef_type()).isEqualTo("P");
        assertThat(aCase.getDef_sex()).isEqualTo("F");
        assertThat(aCase.getDef_age()).isEqualTo("14");
        assertThat(aCase.getDef_addr()).isEqualToComparingFieldByField(Address.builder()
                                                                    .line1("39 The Stree")
                                                                    .line2("Newtown")
                                                                    .pcode("NT4 6YH").build());
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
        assertThat(offence.getPleaDate()).isEqualTo("28/09/2016");
        assertThat(offence.getConvdate()).isEqualTo("28/09/2016");
    }

}
