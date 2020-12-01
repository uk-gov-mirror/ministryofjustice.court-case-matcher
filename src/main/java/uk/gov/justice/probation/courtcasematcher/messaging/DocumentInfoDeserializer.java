package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;

import static uk.gov.justice.probation.courtcasematcher.messaging.OuCodeDeserializer.OU_CODE_LENGTH;

@Slf4j
@Component
public class DocumentInfoDeserializer extends StdDeserializer<Info> {

    private static final String FIELD_DELIM = "_";

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");

    public DocumentInfoDeserializer() {
        this(String.class);
    }

    public DocumentInfoDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Info deserialize(JsonParser jp, DeserializationContext context) throws IOException {
        final String sourceFileName =  Optional.ofNullable(jp.getCodec().readTree(jp))
            .map((treeNode) -> ((JsonNode) treeNode).get(Info.SOURCE_FILE_NAME_ELEMENT))
            .map(JsonNode::asText)
            .orElse("");

        // Source filename has the following format 146_27072020_2578_B01OB00_ADULT_COURT_LIST_DAILY
        final String[] fileNameParts = sourceFileName.split(FIELD_DELIM);
        if (fileNameParts.length < 4) {
            log.error("Unable to determine OU code and date of hearing from source file name of {}", sourceFileName);
            return Info.builder().build();
        }

        String ouCode = fileNameParts[3].toUpperCase();
        if (ouCode.length() > OU_CODE_LENGTH) {
            ouCode = ouCode.substring(0, OU_CODE_LENGTH);
        }
        final long seq = Long.parseLong(fileNameParts[0]);

        try {
            final LocalDate dateOfHearing = LocalDate.parse(fileNameParts[1], formatter);
            return Info.builder().sequence(seq).dateOfHearing(dateOfHearing).ouCode(ouCode).build();
        }
        catch ( DateTimeParseException ex) {
            return Info.builder().build();
        }
    }
}
