package uk.gov.justice.probation.courtcasematcher.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    @Value("${case-mapper-reference.defaultCourtCode}")
    private String defaultCourtCode;

    public CaseMapperReference() {
        super();
        CaseMapperReference.instance = this;
    }

    private final Map<String, String> courtNameToCodes = new HashMap<>();

    public void setCourtNameToCodes(Map<String, String> courtNameToCodes) {
        this.courtNameToCodes.putAll(courtNameToCodes);
    }

    public void setDefaultProbationStatus(String defaultProbationStatus) {
        this.defaultProbationStatus = defaultProbationStatus;
    }

    public Optional<String> getCourtCodeFromName(String courtName) {
        return Optional.ofNullable(courtNameToCodes.get(courtName.replaceAll("\\s+","")));
    }

    public String getDefaultProbationStatus() {
        return defaultProbationStatus;
    }

    public String getDefaultCourtCode() {
        return defaultCourtCode;
    }
}
