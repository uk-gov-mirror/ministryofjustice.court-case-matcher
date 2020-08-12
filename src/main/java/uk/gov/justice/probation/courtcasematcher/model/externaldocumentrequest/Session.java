package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
@JsonIgnoreProperties(value = {"lja", "cmu", "area_code", "panel"})
public class Session {

    @NotNull
    @JacksonXmlProperty(localName = "s_id")
    private final Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", lenient = OptBoolean.TRUE)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JacksonXmlProperty(localName = "doh")
    @NotNull
    private final LocalDate dateOfHearing;

    @NotNull
    @JacksonXmlProperty(localName = "court")
    private final String courtName;
    @NotNull
    @JacksonXmlProperty(localName = "room")
    private final String courtRoom;
    @JacksonXmlProperty(localName = "ou_code")
    private final String courtCode;


    @NotNull
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JacksonXmlProperty(localName = "sstart")
    private final LocalTime start;

    @NotNull
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JacksonXmlProperty(localName = "send")
    private final LocalTime end;

    @JsonManagedReference
    @JacksonXmlElementWrapper
    @ToString.Exclude
    @NotEmpty
    private final List<@Valid Block> blocks;

    // This is a temporary measure for tactical solution. ouCode will be available in this object in the longer term.
    public String getCourtCode() {
        return courtCode != null ? courtCode : job.getDataJob().getDocument().getInfo().getInfoSourceDetail().getOuCode();
    }

    @JsonBackReference
    private final Job job;

    public LocalDateTime getSessionStartTime() {
        //noinspection ConstantConditions - analysis says these fields may be null but annotations / validation prevents that
        return LocalDateTime.of(dateOfHearing, start);
    }
}
