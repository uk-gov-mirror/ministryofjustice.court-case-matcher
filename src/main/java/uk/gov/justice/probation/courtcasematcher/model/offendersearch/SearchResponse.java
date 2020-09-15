package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import lombok.*;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@ToString
public class SearchResponse {
    private final List<Match> matches;
    private final OffenderSearchMatchType matchedBy;
    private final String probationStatus;
}
