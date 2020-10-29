package uk.gov.justice.probation.courtcasematcher.model.offendersearch;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class MatchRequest {

    private String pncNumber;
    private String firstName;
    private String surname;
    private String dateOfBirth;

    @Slf4j
    @Component
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Factory {
        private static final String ERROR_NO_DATE_OF_BIRTH = "No dateOfBirth provided";
        private static final String ERROR_NO_NAME = "No surname provided";
        @Autowired
        private NameHelper nameHelper;

        public MatchRequest buildFrom(String pnc, Name fullName, LocalDate dateOfBirth) throws IllegalArgumentException {
            if (dateOfBirth == null) {
                log.error(ERROR_NO_DATE_OF_BIRTH);
                throw new IllegalArgumentException(ERROR_NO_DATE_OF_BIRTH);
            }

            if (fullName == null || StringUtils.isEmpty(fullName.getSurname())) {
                log.error(ERROR_NO_NAME);
                throw new IllegalArgumentException(ERROR_NO_NAME);
            }

            MatchRequestBuilder builder = builder()
                                                        .pncNumber(pnc)
                                                        .surname(fullName.getSurname())
                                                        .dateOfBirth(dateOfBirth.format(DateTimeFormatter.ISO_DATE));
            String forenames = fullName.getForenames();
            if (!StringUtils.isEmpty(forenames)) {
                builder.firstName(forenames);
            }
            return builder.build();
        }

        public MatchRequest buildFrom(CourtCase courtCase) throws IllegalArgumentException {
            Name defendantName = Optional.ofNullable(courtCase.getName())
                    .orElseGet(() -> nameHelper.getNameFromFields(courtCase.getDefendantName()));
            return buildFrom(courtCase.getPnc(), defendantName, courtCase.getDefendantDob());
        }
    }
}
