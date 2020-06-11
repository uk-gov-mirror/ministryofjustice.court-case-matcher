package uk.gov.justice.probation.courtcasematcher.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourtCase implements Serializable {

    private final String caseId;

    @Setter(AccessLevel.NONE)
    private final String caseNo;

    @Setter(AccessLevel.NONE)
    private final String courtCode;

    private final String courtRoom;

    private final LocalDateTime sessionStartTime;

    private final String probationStatus;

    private final List<Offence> offences;

    private final String crn;

    private final String pnc;

    private final String defendantName;

    private final Address defendantAddress;

    private final LocalDate defendantDob;

    private final String defendantSex;

    private final String listNo;

    private final String nationality1;

    private final String nationality2;

    private final Boolean breach;

    private final Boolean suspendedSentenceOrder;

}
