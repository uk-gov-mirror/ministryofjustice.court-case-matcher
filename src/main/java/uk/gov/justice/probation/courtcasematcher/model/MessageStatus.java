package uk.gov.justice.probation.courtcasematcher.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Data
@Builder
public class MessageStatus {

    private final String status;
    private final String code;
    private final String reason;
    private final String detail;

}
