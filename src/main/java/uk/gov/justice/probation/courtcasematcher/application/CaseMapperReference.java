package uk.gov.justice.probation.courtcasematcher.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "case-mapper-reference")
public class CaseMapperReference {

    public static CaseMapperReference instance;

    @Value("${case-mapper-reference.defaultProbationStatus}")
    private String defaultProbationStatus;

    public CaseMapperReference() {
        super();
        CaseMapperReference.instance = this;
    }

    public void setDefaultProbationStatus(String defaultProbationStatus) {
        this.defaultProbationStatus = defaultProbationStatus;
    }

    public String getDefaultProbationStatus() {
        return defaultProbationStatus;
    }
}
