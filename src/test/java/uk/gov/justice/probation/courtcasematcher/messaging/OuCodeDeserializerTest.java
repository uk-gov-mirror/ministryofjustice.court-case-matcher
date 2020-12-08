package uk.gov.justice.probation.courtcasematcher.messaging;


import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OuCodeDeserializerTest {

    @Mock
    private JsonNode jsonNode;
    @Mock
    private JsonParser jsonParser;
    @Mock
    private ObjectCodec objectCodec;
    @Mock
    private DeserializationContext context;

    private OuCodeDeserializer deserializer;

    @BeforeEach
    void beforeEach() {
        deserializer = new OuCodeDeserializer();
    }

    @DisplayName("Element is populated with a normal string and OU code is truncated")
    @Test
    void whenNodePopulated_thenReturn() throws IOException {

        when(jsonParser.getCodec()).thenReturn(objectCodec);
        when(objectCodec.readTree(jsonParser)).thenReturn(jsonNode);
        when(jsonNode.asText()).thenReturn("B01OB00");

        final String ouCode = deserializer.deserialize(jsonParser, context);

        assertThat(ouCode).isEqualTo("B01OB");
    }

    @DisplayName("There is no element so return empty string")
    @Test
    void whenNodeNull_thenReturnEmpty() throws IOException {

        when(jsonParser.getCodec()).thenReturn(objectCodec);
        when(objectCodec.readTree(jsonParser)).thenReturn(null);

        final String ouCode = deserializer.deserialize(jsonParser, context);

        assertThat(ouCode).isEqualTo("");
    }

    @DisplayName("The element content is null so return empty string")
    @Test
    void whenNodeContentIsNull_thenReturnEmpty() throws IOException {

        when(jsonParser.getCodec()).thenReturn(objectCodec);
        when(objectCodec.readTree(jsonParser)).thenReturn(jsonNode);
        when(jsonNode.asText()).thenReturn(null);

        final String ouCode = deserializer.deserialize(jsonParser, context);

        assertThat(ouCode).isEqualTo("");
    }

}
