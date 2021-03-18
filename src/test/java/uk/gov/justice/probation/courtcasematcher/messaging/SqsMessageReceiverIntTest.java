package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
public class SqsMessageReceiverIntTest {

    private static String singleCaseXml;

    @Autowired
    private TelemetryService telemetryService;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @BeforeAll
    static void beforeAll() throws IOException {
        final String basePath = "src/test/resources/messages";
        singleCaseXml = Files.readString(Paths.get(basePath +"/external-document-request-single-case.xml"));
        MOCK_SERVER.start();
    }

    @AfterAll
    static void afterAll() {
        MOCK_SERVER.stop();
    }

    private static final String QUEUE_NAME = "crime-portal-gateway-queue";

    @Autowired
    private QueueMessagingTemplate queueMessagingTemplate;

    @Test
    public void whenReceivePayload_thenSendCase() {

        queueMessagingTemplate.convertAndSend(QUEUE_NAME, singleCaseXml);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> countPutRequestsTo("/court/B10JQ/case/1600032981") == 1);

        MOCK_SERVER.verify(
            putRequestedFor(urlEqualTo("/court/B10JQ/case/1600032981"))
                .withRequestBody(matchingJsonPath("pnc", equalTo("2004/0012345U")))
        );

        verify(telemetryService).trackSQSMessageEvent(any(String.class));
        verify(telemetryService).trackCourtCaseEvent(any(Case.class), any(String.class));
        verify(telemetryService).trackOffenderMatchEvent(any(CourtCase.class), any(SearchResponse.class));
        verify(telemetryService).trackCourtListEvent(any(Info.class), any(String.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @TestConfiguration
    public static class AwsTestConfig {
        @Value("${aws.sqs-endpoint-url}")
        private String sqsEndpointUrl;
        @Value("${aws.access_key_id}")
        private String accessKeyId;
        @Value("${aws.secret_access_key}")
        private String secretAccessKey;
        @Value("${aws.region_name}")
        private String regionName;
        @Value("${aws.sqs.queue_name}")
        private String queueName;
        @Autowired
        private EventBus eventBus;
        @MockBean
        private TelemetryService telemetryService;
        @Autowired
        private MessageParser messageParser;
        @Autowired
        private MessageProcessor messageProcessor;

        @Primary
        @Bean
        public AmazonSQSAsync amazonSQSAsync() {
            return AmazonSQSAsyncClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey)))
                .withEndpointConfiguration(new EndpointConfiguration(sqsEndpointUrl, regionName))
                .build();
        }

        @Bean
        public SqsMessageReceiver sqsMessageReceiver() {
            return new SqsMessageReceiver(messageProcessor, telemetryService, eventBus, messageParser, queueName);
        }

        @Bean
        public QueueMessagingTemplate queueMessagingTemplate(@Autowired AmazonSQSAsync amazonSQSAsync) {
            return new QueueMessagingTemplate(amazonSQSAsync);
        }
    }

    public int countPutRequestsTo(final String url) {
        return MOCK_SERVER.findAll(putRequestedFor(urlEqualTo(url))).size();
    }

}
