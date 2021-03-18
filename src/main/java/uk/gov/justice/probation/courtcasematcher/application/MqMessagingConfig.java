package uk.gov.justice.probation.courtcasematcher.application;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import uk.gov.justice.probation.courtcasematcher.messaging.JmsErrorHandler;

@Profile("mq-messaging")
@Configuration
public class MqMessagingConfig {

    @Autowired
    private JmsErrorHandler jmsErrorHandler;

    @Bean(name = "mqJmsListenerContainerFactory")
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory (ActiveMQConnectionFactory jmsConnectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(jmsConnectionFactory);
        factory.setSessionTransacted(Boolean.TRUE);
        factory.setErrorHandler(jmsErrorHandler);
        return factory;
    }
}
