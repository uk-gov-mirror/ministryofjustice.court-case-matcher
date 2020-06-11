package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.application.CaseMapperReference;

@Component
public class CourtDeserializer extends StdDeserializer<String> {

    private CaseMapperReference caseMapperReference;

    public CourtDeserializer() {
        this(String.class);
        this.caseMapperReference = CaseMapperReference.instance;
    }

    public CourtDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String courtName = node != null ? node.asText() : "";
        return caseMapperReference.getCourtCodeFromName(courtName)
            .orElseThrow(() -> new JsonParseException(jp, "Unable to find court code for court name :" + courtName));
    }
}
