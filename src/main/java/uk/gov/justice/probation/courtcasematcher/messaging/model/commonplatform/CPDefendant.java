package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.type.DefendantType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Slf4j
public class CPDefendant {
    private static final String PNC_REGEX = "^[0-9]{4}[0-9]{7}[a-zA-Z]$";
    @NotBlank
    private final String id;
    private final String pncId;
    private final String croNumber;
    @NotBlank
    private final String prosecutionCaseId;
    @NotNull
    @Valid
    private final List<CPOffence> offences;
    @Valid
    private final CPPersonDefendant personDefendant;
    @Valid
    private final CPLegalEntityDefendant legalEntityDefendant;
    @JsonProperty(value="isYouth")
    private final boolean isYouth;

    public Defendant asDomain() {
        if (personDefendant == null && legalEntityDefendant == null) {
            throw new IllegalStateException(String.format("Defendant with id '%s' is neither a person nor a legal entity", getId()));
        }

        return Optional.of(this)
                .filter(cpDefendant -> !cpDefendant.isYouth) //do not include youth defendants
                .map(CPDefendant::getPersonDefendant)
                .map(CPPersonDefendant::getPersonDetails)
                .map(personDetails -> commonFieldsBuilder()
                        .type(DefendantType.PERSON)
                        .dateOfBirth(personDetails.getDateOfBirth())
                        .name(personDetails.asName())
                        .sex(personDetails.getGender())
                        .address(Optional.ofNullable(personDetails.getAddress())
                                .map(CPAddress::asDomain).orElse(Address.builder().build()))
                        .phoneNumber(Optional.ofNullable(personDetails.getContact())
                                .map(CPContact::asPhoneNumber).orElse(null))
                        .build())
                .orElseGet(this::getLegalDefendant);
    }

    private Defendant.DefendantBuilder commonFieldsBuilder() {
        return Defendant.builder()
                .defendantId(getId())
                .pnc(correctPnc(getPncId()))
                .cro(getCroNumber())
                .offences(getOffences().stream()
                        .map(CPOffence::asDomain)
                        .collect(Collectors.toList()));
    }

    public static String correctPnc(String pncId) {
        return Optional.ofNullable(pncId)
                .filter(s -> s.matches(PNC_REGEX))
                .map(s -> {
                    final var year = pncId.substring(0, 4);
                    final var id = pncId.substring(4, 12);
                    final var corrected = String.format("%s/%s", year, id);
                    log.debug("Correcting PNC {} to {}", pncId, corrected);
                    return corrected;
                }).orElseGet(() -> {
                    log.debug("Will not correct PNC {} as it is not in the expected format {}", pncId, PNC_REGEX);
                    return pncId;
                });
    }

    private Defendant getLegalDefendant() {
        return Optional.ofNullable(legalEntityDefendant)
            .map(legalEntityDefendant -> commonFieldsBuilder()
                .type(DefendantType.ORGANISATION)
                .name(getLegalEntityDefendant()
                    .getOrganisation()
                    .asName())
                .build()).orElse(null);
    }
}
