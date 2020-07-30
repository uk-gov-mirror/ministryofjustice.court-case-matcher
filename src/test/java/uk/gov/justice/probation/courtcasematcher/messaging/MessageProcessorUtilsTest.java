package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.DataJob;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Document;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.InfoSourceDetail;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Job;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.probation.courtcasematcher.messaging.MessageProcessorUtils.getCases;
import static uk.gov.justice.probation.courtcasematcher.messaging.MessageProcessorUtils.getCourtCodes;
import static uk.gov.justice.probation.courtcasematcher.messaging.MessageProcessorUtils.getHearingDates;
import static uk.gov.justice.probation.courtcasematcher.messaging.MessageProcessorUtils.getUniqueDocuments;

class MessageProcessorUtilsTest {

    private static final int DAY_OFFSET = 3;
    private static GatewayMessageParser parser;

    @BeforeAll
    static void beforeAll() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        XmlMapper mapper = new XmlMapper(xmlModule);
        mapper.registerModule(new JavaTimeModule());
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        parser =  new GatewayMessageParser(mapper, factory.getValidator());
    }

    @DisplayName("Gets distinct set of hearing dates sorted")
    @Test
    void whenSessionsProvidedForBothDatesThenReturn() {

        LocalDate now = LocalDate.now();
        Session sessionToday1 = Session.builder().courtCode("SHF").dateOfHearing(now).courtRoom("1").build();
        Session sessionToday2 = Session.builder().courtCode("SHF").dateOfHearing(now).courtRoom("2").build();
        Session sessionFuture1 = Session.builder().courtCode("SHF").dateOfHearing(now.plusDays(3)).courtRoom("1").build();
        Session sessionFuture2 = Session.builder().courtCode("SHF").dateOfHearing(now.plusDays(3)).courtRoom("2").build();

        TreeSet<LocalDate> hearingDates = getHearingDates(Arrays.asList(sessionFuture1, sessionToday2, sessionFuture2, sessionToday1), DAY_OFFSET);
        assertThat(hearingDates).hasSize(2);
        assertThat(hearingDates.first()).isEqualTo(now);
        assertThat(hearingDates.last()).isEqualTo(now.plusDays(DAY_OFFSET));
    }

    @DisplayName("Gets distinct set of hearing dates sorted, despite only having input for today")
    @Test
    void whenSessionsProvidedForTodayOnly_ThenReturnTodayAndDerived() {

        LocalDate now = LocalDate.now();
        Session sessionToday1 = Session.builder().courtCode("SHF").dateOfHearing(now).courtRoom("1").build();
        Session sessionToday2 = Session.builder().courtCode("SHF").dateOfHearing(now).courtRoom("2").build();

        TreeSet<LocalDate> hearingDates = getHearingDates(Arrays.asList(sessionToday2, sessionToday1), DAY_OFFSET);
        assertThat(hearingDates).hasSize(2);
        assertThat(hearingDates.first()).isEqualTo(now);
        assertThat(hearingDates.last()).isEqualTo(now.plusDays(DAY_OFFSET));
    }

    @DisplayName("Gets distinct set of hearing dates sorted, despite only having input for the future date")
    @Test
    void whenSessionsProvidedForFutureOnly_ThenReturnTodayAndDerived() {

        LocalDate future = LocalDate.now().plusDays(DAY_OFFSET);
        Session session1 = Session.builder().courtCode("SHF").dateOfHearing(future).courtRoom("1").build();
        Session session2 = Session.builder().courtCode("SHF").dateOfHearing(future).courtRoom("2").build();

        TreeSet<LocalDate> hearingDates = getHearingDates(Arrays.asList(session2, session1), DAY_OFFSET);
        assertThat(hearingDates).hasSize(2);
        assertThat(hearingDates.first()).isEqualTo(LocalDate.now());
        assertThat(hearingDates.last()).isEqualTo(future);
    }

    @DisplayName("Although unexpected, three distinct dates will still work.")
    @Test
    void whenSessionsProvidedForThreeDates_ThenReturnAll() {

        LocalDate today = LocalDate.now();
        LocalDate future = today.plusDays(DAY_OFFSET);
        Session sessionToday = Session.builder().courtCode("SHF").dateOfHearing(today).courtRoom("1").build();
        Session sessionFuture1 = Session.builder().courtCode("SHF").dateOfHearing(future).courtRoom("2").build();
        Session sessionFuture2 = Session.builder().courtCode("SHF").dateOfHearing(future.plusDays(1)).courtRoom("2").build();

        TreeSet<LocalDate> hearingDates = getHearingDates(Arrays.asList(sessionToday, sessionFuture1, sessionFuture2), DAY_OFFSET);
        assertThat(hearingDates).hasSize(3);
        assertThat(hearingDates.first()).isEqualTo(LocalDate.now());
        assertThat(hearingDates.last()).isEqualTo(future.plusDays(1));
    }

    @DisplayName("Gets list of the cases for a court")
    @Test
    void whenGetCasesForACourt() {

        Session session1 = Session.builder().courtName("Camberwell Green")
            .job(buildJob("B01CX00"))
            .blocks(Arrays.asList(
                Block.builder().cases(Arrays.asList(buildCase("100000001"), buildCase("100000002"))).build(),
                Block.builder().cases(Arrays.asList(buildCase("100000003"), buildCase("100000004"))).build()
            )).build();
        Session session2 = Session.builder().courtName("Beverley")
            .job(buildJob("B16BG00"))
            .blocks(singletonList(
                Block.builder().cases(Arrays.asList(buildCase("10000005"), buildCase("100000006"))).build()
            )).build();

        List<Case> cases = getCases("B01CX00", Arrays.asList(session1, session2));

        assertThat(cases).hasSize(4);
        assertThat(cases).extracting("caseNo").contains("100000001", "100000002", "100000003", "100000004");
    }

    @DisplayName("Gets list of the cases for a court")
    @Test
    void whenGetCourtCodesFromSessions() {
        Session session1 = Session.builder()
            .job(buildJob("B16BG00"))
            .courtRoom("1")
            .build();
        Session session2 = Session.builder()
            .job(buildJob("B16BG00"))
            .courtRoom("1")
            .build();
        Session session3 = Session.builder()
            .job(buildJob("B01CX00"))
            .build();

        Set<String> courtCodes = getCourtCodes(Arrays.asList(session1, session2, session3));

        assertThat(courtCodes).hasSize(2);
        assertThat(courtCodes).contains("B16BG00", "B01CX00");
    }

    @DisplayName("Filters a list of 23 input documents to the 4 with highest sequence numbers")
    @Test
    void givenDuplicateDocuments_ThenDeDuplicate() throws IOException {

        LocalDate july28 = LocalDate.of(2020, Month.JULY, 28);
        LocalDate july31 = LocalDate.of(2020, Month.JULY, 31);

        List<Document> inputDocuments = getDocumentsFromSample("src/test/resources/messages/gateway-message-duplicates.xml");
        assertThat(inputDocuments).hasSize(23);

        List<Document> documents = getUniqueDocuments(inputDocuments);

        assertThat(documents).hasSize(4);

        assertThat(findDocumentSequenceNumber(documents, "B16BG00", july28)).isEqualTo(182);
        assertThat(findDocumentSequenceNumber(documents, "B01CX00", july28)).isEqualTo(179);
        assertThat(findDocumentSequenceNumber(documents, "B01OB00", july28)).isEqualTo(174);
        assertThat(findDocumentSequenceNumber(documents, "B01CX00", july31)).isEqualTo(180);
    }

    @DisplayName("Filters a list of documents where there are no duplicates")
    @Test
    void givenNoDuplicateDocuments_ThenReturnSame() throws IOException {

        List<Document> inputDocuments = getDocumentsFromSample("src/test/resources/messages/gateway-message-multi-day.xml");

        List<Document> documents = getUniqueDocuments(inputDocuments);

        assertThat(documents).hasSize(2);
    }

    private long findDocumentSequenceNumber(List<Document> documents, String courtCode, LocalDate hearingDate) {
        return documents.stream()
            .filter(document -> (document.getInfo().getDateOfHearing().equals(hearingDate)
                                && document.getInfo().getInfoSourceDetail().getOuCode().equals(courtCode)))
            .findFirst()
            .map(document -> document.getInfo().getInfoSourceDetail().getSequence())
            .orElse(-1L);
    }

    private List<Document> getDocumentsFromSample(String path) throws IOException {
        String content = Files.readString(Paths.get(path));
        return parser.parseMessage(content).getMessageBody().getGatewayOperationType().getExternalDocumentRequest().getDocumentWrapper().getDocument();
    }

    private Case buildCase(String caseNo) {
        return Case.builder().caseNo(caseNo).build();
    }

    private Job buildJob(String ouCode) {
        Info info = Info.builder().infoSourceDetail(InfoSourceDetail.builder().ouCode(ouCode).build()).build();
        Document document = Document.builder().info(info).build();
        DataJob dataJob = DataJob.builder().document(document).build();
        return Job.builder().dataJob(dataJob).build();
    }
}
