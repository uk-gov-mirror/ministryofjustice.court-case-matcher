package uk.gov.justice.probation.courtcasematcher.model.courtcaseservice;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import reactor.util.StringUtils;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;

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

    private final String probationStatusActual;

    private final List<Offence> offences;

    private final String crn;

    private final String cro;

    private final String pnc;

    private final Name name;

    private final String defendantName;

    private final Address defendantAddress;

    private final LocalDate defendantDob;

    private final DefendantType defendantType;

    private final String defendantSex;

    private final String listNo;

    private final String nationality1;

    private final String nationality2;

    private final Boolean breach;

    private final LocalDate previouslyKnownTerminationDate;

    private final Boolean suspendedSentenceOrder;

    private final boolean preSentenceActivity;

    @JsonIgnore
    private final GroupedOffenderMatches groupedOffenderMatches;

    @JsonIgnore
    private final boolean isNew;

    public boolean isPerson() {
        return Optional.ofNullable(defendantType).map(defType -> defType == DefendantType.PERSON).orElse(false);
    }

    public boolean shouldMatchToOffender() {
        return isPerson() && !StringUtils.hasText(crn);
    }
}
