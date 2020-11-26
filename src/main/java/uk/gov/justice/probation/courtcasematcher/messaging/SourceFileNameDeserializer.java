package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.InfoSourceDetail;

import static uk.gov.justice.probation.courtcasematcher.messaging.OuCodeDeserializer.OU_CODE_LENGTH;

@Slf4j
@Component
public class SourceFileNameDeserializer extends StdDeserializer<InfoSourceDetail> {

    public SourceFileNameDeserializer() {
        this(String.class);
    }

    public SourceFileNameDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public InfoSourceDetail deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String sourceFileName = node != null ? node.asText() : "";
        // Source filename has the following format 146_27072020_2578_B01OB00_ADULT_COURT_LIST_DAILY
        String[] fileNameParts = sourceFileName.split("_");
        if (fileNameParts.length < 4) {
            log.error("Unable to determine OU code from source file name of {}", sourceFileName);
            return InfoSourceDetail.builder().build();
        }

        String ouCode = fileNameParts[3].toUpperCase();
        if (ouCode.length() > OU_CODE_LENGTH) {
            ouCode = ouCode.substring(0, OU_CODE_LENGTH);
        }
        long seq = Long.parseLong(fileNameParts[0]);
        log.debug("Got OU code of {} and identifier of {} from source_file_name of {}", ouCode, seq, sourceFileName);
        return InfoSourceDetail.builder().ouCode(ouCode).sequence(seq).build();
    }
}
