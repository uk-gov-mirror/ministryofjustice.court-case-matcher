package uk.gov.justice.probation.courtcasematcher.model.courtcaseservice;

import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OtherIds;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class OffenderDetail {
    private OtherIds otherIds;
    private String title;
    private String forename;
    private List<String> middleNames;
    private String surname;
    private LocalDate dateOfBirth;
    private String probationStatus;
}
