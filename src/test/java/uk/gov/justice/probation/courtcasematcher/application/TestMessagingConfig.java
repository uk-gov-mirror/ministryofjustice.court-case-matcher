package uk.gov.justice.probation.courtcasematcher.application;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.boot.actuate.jms.JmsHealthIndicator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestMessagingConfig {

    @Bean
    public ActiveMQConnectionFactory jmsConnectionFactory() {
        return mock(ActiveMQConnectionFactory.class);
    }

    @Bean
    public JmsHealthIndicator jmsHealthIndicator() {
        return mock(JmsHealthIndicator.class);
    }

}
