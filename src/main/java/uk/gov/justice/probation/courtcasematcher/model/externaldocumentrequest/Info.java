
package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import java.time.LocalDate;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Info {

    public static final String SOURCE_FILE_NAME_ELEMENT = "source_file_name";

    @EqualsAndHashCode.Exclude
    private final Long sequence;

    @NotNull
    private final String ouCode;

    @NotNull
    private final LocalDate dateOfHearing;

}
