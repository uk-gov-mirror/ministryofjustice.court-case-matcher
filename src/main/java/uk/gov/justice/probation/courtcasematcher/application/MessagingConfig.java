package uk.gov.justice.probation.courtcasematcher.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.eventbus.EventBus;
import javax.validation.Validation;
import javax.validation.Validator;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import uk.gov.justice.probation.courtcasematcher.messaging.JmsErrorHandler;


@Configuration
public class MessagingConfig {

    @Autowired
    private JmsErrorHandler jmsErrorHandler;

    // Without this, Spring uses the XmlMapper bean as the ObjectMapper for the whole app and we get actuator response as XML
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean(name = "gatewayMessageXmlMapper")
    public XmlMapper xmlMapper() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        XmlMapper mapper = new XmlMapper(xmlModule);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory (@Autowired ActiveMQConnectionFactory jmsConnectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(jmsConnectionFactory);
        factory.setSessionTransacted(Boolean.TRUE);
        factory.setErrorHandler(jmsErrorHandler);
        return factory;
    }

}
