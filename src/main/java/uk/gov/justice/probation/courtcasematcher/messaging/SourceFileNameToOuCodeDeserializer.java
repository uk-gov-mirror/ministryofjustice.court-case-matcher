package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SourceFileNameToOuCodeDeserializer extends StdDeserializer<String> {

    public SourceFileNameToOuCodeDeserializer() {
        this(String.class);
    }

    public SourceFileNameToOuCodeDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String sourceFileName = node != null ? node.asText() : "";
        // Source filename has the following format 146_27072020_2578_B01OB00_ADULT_COURT_LIST_DAILY
        String[] fileNameParts = sourceFileName.split("_");
        if (fileNameParts.length < 4) {
            log.error("Unable to determine OU code from source file name of {}", sourceFileName);
            return "";
        }

        log.debug("Got OU code of {} from source_file_name of {}", fileNameParts[3], sourceFileName);
        return fileNameParts[3].toUpperCase();
    }
}
