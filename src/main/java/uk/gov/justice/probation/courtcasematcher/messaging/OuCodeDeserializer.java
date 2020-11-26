package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Takes the OU code as provided by the feed, a string of 8 characters "B01OB01"
 * and truncates it to remove the room digits which we are told are the last
 * two characters, leaving "B01OB".
 */
@Slf4j
@Component
public class OuCodeDeserializer extends StdDeserializer<String> {

    static final int OU_CODE_LENGTH = 5;

    public OuCodeDeserializer() {
        this(String.class);
    }

    public OuCodeDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        final JsonNode node = jp.getCodec().readTree(jp);
        final String ouCodeWithRoom = node != null ? node.asText() : "";
        if (ouCodeWithRoom.length() <= OU_CODE_LENGTH) {
            return ouCodeWithRoom;
        }

        final String ouCode = ouCodeWithRoom.substring(0, OU_CODE_LENGTH);
        log.debug("Got OU code of {} from input of {}", ouCode, ouCodeWithRoom);
        return ouCode.substring(0, OU_CODE_LENGTH);
    }

}
