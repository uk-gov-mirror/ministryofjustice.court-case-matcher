package uk.gov.justice.probation.courtcasematcher.messaging;


import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
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
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentInfoDeserializerTest {

    @Mock
    private JsonNode jsonNode;
    @Mock
    private JsonNode sourceFileNameNode;
    @Mock
    private JsonParser jsonParser;
    @Mock
    private ObjectCodec objectCodec;
    @Mock
    private DeserializationContext context;

    private DocumentInfoDeserializer deserializer;

    @BeforeEach
    void beforeEach() {
        deserializer = new DocumentInfoDeserializer();
    }

    @DisplayName("Element is populated with a normal string and Info created")
    @Test
    void whenNodePopulated_thenExtractInfo() throws IOException {

        when(jsonParser.getCodec()).thenReturn(objectCodec);
        when(objectCodec.readTree(jsonParser)).thenReturn(jsonNode);
        when(jsonNode.get(Info.SOURCE_FILE_NAME_ELEMENT)).thenReturn(sourceFileNameNode);
        when(sourceFileNameNode.asText()).thenReturn("146_27072020_2578_B01OB00_ADULT_COURT_LIST_DAILY");

        final Info info = deserializer.deserialize(jsonParser, context);

        assertThat(info.getOuCode()).isEqualTo("B01OB");
        assertThat(info.getDateOfHearing()).isEqualTo(LocalDate.of(2020, Month.JULY, 27));
        assertThat(info.getSequence()).isEqualTo(146);
    }

    @DisplayName("Element is populated with a string with invalid date")
    @Test
    void whenNodeDateIncorrect_thenReturnEmptyInfo() throws IOException {

        when(jsonParser.getCodec()).thenReturn(objectCodec);
        when(objectCodec.readTree(jsonParser)).thenReturn(jsonNode);
        when(jsonNode.get(Info.SOURCE_FILE_NAME_ELEMENT)).thenReturn(sourceFileNameNode);
        when(sourceFileNameNode.asText()).thenReturn("146_2707020_2578_B01OB00_ADULT_COURT_LIST_DAILY");

        final Info info = deserializer.deserialize(jsonParser, context);

        assertThat(info.getOuCode()).isNull();
        assertThat(info.getDateOfHearing()).isNull();
        assertThat(info.getSequence()).isNull();
    }

    @DisplayName("Null node present in tree")
    @Test
    void whenNullInTree_thenReturnEmptyInfo() throws IOException {

        when(jsonParser.getCodec()).thenReturn(objectCodec);
        when(objectCodec.readTree(jsonParser)).thenReturn(null);

        final Info info = deserializer.deserialize(jsonParser, context);

        assertThat(info.getOuCode()).isNull();
        assertThat(info.getDateOfHearing()).isNull();
        assertThat(info.getSequence()).isNull();
    }

    @DisplayName("Null node present in text node")
    @Test
    void whenNullForTextNode_thenReturnEmptyInfo() throws IOException {

        when(jsonParser.getCodec()).thenReturn(objectCodec);
        when(objectCodec.readTree(jsonParser)).thenReturn(jsonNode);
        when(jsonNode.get(Info.SOURCE_FILE_NAME_ELEMENT)).thenReturn(null);

        final Info info = deserializer.deserialize(jsonParser, context);

        assertThat(info.getOuCode()).isNull();
        assertThat(info.getDateOfHearing()).isNull();
        assertThat(info.getSequence()).isNull();
    }

    @DisplayName("Null content text node")
    @Test
    void whenNullInContentOfTextNode_thenReturnEmptyInfo() throws IOException {

        when(jsonParser.getCodec()).thenReturn(objectCodec);
        when(objectCodec.readTree(jsonParser)).thenReturn(jsonNode);
        when(jsonNode.get(Info.SOURCE_FILE_NAME_ELEMENT)).thenReturn(sourceFileNameNode);
        when(sourceFileNameNode.asText()).thenReturn(null);

        final Info info = deserializer.deserialize(jsonParser, context);

        assertThat(info.getOuCode()).isNull();
        assertThat(info.getDateOfHearing()).isNull();
        assertThat(info.getSequence()).isNull();
    }

}
