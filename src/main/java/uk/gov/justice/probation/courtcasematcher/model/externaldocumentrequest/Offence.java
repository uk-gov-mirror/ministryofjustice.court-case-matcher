package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
@JsonIgnoreProperties(value = {"co_id", "code", "maxpen", "cy_title", "alm", "ala", "cy_sum", "cy_as", "sof", "cy_sof",
                                "adjdate", "adjreason", "plea", "pleadate", "convdate"})
public class Offence {

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "oseq")
    private final Integer seq;

    @NotBlank
    private final String sum;
    @NotBlank
    private final String title;
    private final String as;
}
