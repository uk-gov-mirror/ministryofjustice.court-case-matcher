package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Getter
@EqualsAndHashCode
@ToString
public class Name {

    private final String title;
    private final String forename1;
    private final String forename2;
    private final String forename3;
    private final String surname;

    public String getForenames() {
        return Stream.of(forename1, forename2, forename3)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" "))
            .trim();
    }

    public String getFullName() {
        return Stream.of(title, forename1, forename2, forename3, surname)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" "))
            .trim();
    }

}
