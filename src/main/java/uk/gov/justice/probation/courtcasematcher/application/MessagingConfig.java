package uk.gov.justice.probation.courtcasematcher.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.eventbus.EventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MessagingConfig {

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
        return new XmlMapper(xmlModule);
    }

    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }
}
