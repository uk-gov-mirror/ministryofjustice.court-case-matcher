package uk.gov.justice.probation.courtcasematcher.application;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("sqs-messaging")
@Configuration
public class SqsMessagingConfig {

    @EnableAutoConfiguration(exclude = ArtemisAutoConfiguration.class)
    private static class ArtemisAutoConfigToggle{}

    @Primary
    @Bean
    public AmazonSQSAsync amazonSQSAsync(@Value("${aws.region-name}") final String regionName,
                                        @Value("${aws.sqs-endpoint-url}") final String awsEndpointUrl,
                                        @Value("${aws_access_key_id}") final String awsAccessKeyId,
                                        @Value("${aws_secret_access_key}") final String awsSecretAccessKey) {
        return AmazonSQSAsyncClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey)))
            .withEndpointConfiguration(new EndpointConfiguration(awsEndpointUrl, regionName))
            .build();
    }

}
