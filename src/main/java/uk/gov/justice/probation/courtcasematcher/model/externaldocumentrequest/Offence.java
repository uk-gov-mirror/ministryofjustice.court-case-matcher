package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Offence {

    private String adjdate;
    private String adjreason;

    private String code;
    @JacksonXmlProperty(localName = "oseq")
    private Integer seq;
    @JacksonXmlProperty(localName = "pleadate")
    private String pleaDate;

    private Long co_id;
    private String convdate;
    private String sum;
    private String title;
    private String plea;
    private String maxpen;
    private String as;
}
