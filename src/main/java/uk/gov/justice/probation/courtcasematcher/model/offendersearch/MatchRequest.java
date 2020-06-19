package uk.gov.justice.probation.courtcasematcher.model.offendersearch;


import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatchRequest {
    private String firstName;
    private String surname;
    private String dateOfBirth;
}
