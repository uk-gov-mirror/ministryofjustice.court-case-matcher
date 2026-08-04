package uk.gov.justice.probation.courtcasematcher.messaging.model.libra;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.mapper.HearingMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
@Valid
public class LibraHearing {

    @NotBlank
    @JacksonXmlProperty(localName = "caseno")
    private final String caseNo;
    private final String urn;
    @PositiveOrZero
    private final Integer seq;
    private final LibraName name;
    private final String defendantName;
    private final String defendantType;
    private final String defendantSex;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", lenient = OptBoolean.TRUE)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private final LocalDate defendantDob;
    private final LibraAddress defendantAddress;
    private final String defendantAge;
    private final String cro;
    private final String pnc;
    private final String listNo;
    private final String nationality1;
    private final String nationality2;

    @ToString.Exclude
    private final List<@Valid LibraOffence> offences;

    private final String courtCode;
    private final String courtRoom;
    private final LocalDateTime sessionStartTime;
    @JsonProperty(value="cId")
    private final String cId;

    public Hearing asDomain() {
        return HearingMapper.newFromLibraHearing(this);
    }
}
