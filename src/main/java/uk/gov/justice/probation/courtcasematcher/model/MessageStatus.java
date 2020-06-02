package uk.gov.justice.probation.courtcasematcher.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class MessageStatus {

    private String status;
    private String code;
    private String reason;
    private String detail;

}
