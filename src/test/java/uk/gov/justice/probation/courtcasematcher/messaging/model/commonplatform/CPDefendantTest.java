package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Plea;
import uk.gov.justice.probation.courtcasematcher.model.domain.Verdict;
import uk.gov.justice.probation.courtcasematcher.model.type.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.domain.PhoneNumber;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPContactTest.TEST_CP_CONTACT;
import static uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPDefendant.correctPnc;

class CPDefendantTest {
    @Test
    public void person_asDomain() {
        final var actual = getCpDefendant()
                .asDomain();

        assertThat(actual.getDefendantId()).isEqualTo("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9");
        assertThat(actual.getType()).isEqualTo(DefendantType.PERSON);
        assertThat(actual.getName().getForename1()).isEqualTo("firstname");
        assertThat(actual.getDateOfBirth()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(actual.getSex()).isEqualTo("MALE");
        assertThat(actual.getCro()).isEqualTo("croNumber");
        assertThat(actual.getPnc()).isEqualTo("2007/1234557L");
        assertThat(actual.getAddress().getLine1()).isEqualTo("address1");
        assertThat(actual.getOffences().get(0).getId()).isEqualTo("1");
        assertThat(actual.getOffences().get(0).getOffenceCode()).isEqualTo("ABC001");
        assertThat(actual.getOffences().get(0).getPlea()).isEqualTo(Plea.builder().build());
        assertThat(actual.getOffences().get(0).getVerdict()).isEqualTo(Verdict.builder().build());
        assertThat(actual.getOffences().get(1).getId()).isEqualTo("2");
        assertThat(actual.getOffences().get(1).getOffenceCode()).isEqualTo("ABC002");
        assertThat(actual.getOffences().get(1).getPlea()).isEqualTo(Plea.builder().build());
        assertThat(actual.getOffences().get(1).getVerdict()).isEqualTo(Verdict.builder().build());
        assertThat(actual.getPhoneNumber()).isEqualTo(PhoneNumber.builder()
                .work(TEST_CP_CONTACT.getWork())
                .mobile(TEST_CP_CONTACT.getMobile())
                .home(TEST_CP_CONTACT.getHome())
                .build());
    }

    @Test
    public void organisation_asDomain() {
        final var actual = CPDefendant.builder()
                .legalEntityDefendant(CPLegalEntityDefendant.builder()
                        .organisation(CPOrganisation.builder()
                                .name("orgname")
                                .build())
                        .build())
                .offences(List.of(CPOffence.builder().id("1")
                                .judicialResults(List.of(CPJudicialResult.builder()
                                        .build()))
                                .build(),

                        CPOffence.builder().id("2")
                                .judicialResults(List.of(CPJudicialResult.builder()
                                        .build()))
                                .build()))
                .id("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9")
                .pncId("20071234557L")
                .croNumber("croNumber")
                .build()
                .asDomain();

        assertThat(actual.getDefendantId()).isEqualTo("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9");
        assertThat(actual.getType()).isEqualTo(DefendantType.ORGANISATION);
        assertThat(actual.getName().getSurname()).isEqualTo("orgname");
        assertThat(actual.getDateOfBirth()).isNull();
        assertThat(actual.getSex()).isEqualTo("NOT_KNOWN");
        assertThat(actual.getCro()).isEqualTo("croNumber");
        assertThat(actual.getPnc()).isEqualTo("2007/1234557L");
        assertThat(actual.getAddress()).isNull();
        assertThat(actual.getOffences().get(0).getId()).isEqualTo("1");
        assertThat(actual.getOffences().get(1).getId()).isEqualTo("2");
    }

    @Test
    public void defendantWithoutPersonOrLegalEntity_throws() {
        final var defendant = CPDefendant.builder()
                .offences(List.of(CPOffence.builder().id("1").build(), CPOffence.builder().id("2").build()))
                .id("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9")
                .pncId("pncid")
                .croNumber("croNumber")
                .build();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> defendant.asDomain())
                .withMessage("Defendant with id '2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9' is neither a person nor a legal entity");

    }

    @Test
    public void normalisePncId_onlyAddsSlashToOtherwiseValidPnc() {
        assertThat(correctPnc("19871234567L")).isEqualTo("1987/1234567L");
        assertThat(correctPnc(null)).isEqualTo(null);
        assertThat(correctPnc("something unexpected")).isEqualTo("something unexpected");
    }

    @Test
    public void filterOutYouthDefendants() {
        final var actual = CPDefendant.builder()
            .isYouth(true)
            .personDefendant(CPPersonDefendant.builder()
                .personDetails(CPPersonDetails.builder()
                    .gender("MALE")
                    .title("title")
                    .firstName("firstname")
                    .middleName("middlename")
                    .lastName("lastname")
                    .dateOfBirth(LocalDate.of(2021, 1, 1))
                    .address(CPAddress.builder()
                        .address1("address1")
                        .build())
                    .contact(TEST_CP_CONTACT)
                    .build())
                .build())
            .offences(List.of(CPOffence.builder().id("1")
                    .offenceCode("ABC001")
                    .plea(Plea.builder().build())
                    .verdict(Verdict.builder().build())
                    .judicialResults(List.of(CPJudicialResult.builder()
                        .build()))
                    .build(),
                CPOffence.builder().id("2")
                    .offenceCode("ABC002")
                    .plea(Plea.builder().build())
                    .verdict(Verdict.builder().build())
                    .judicialResults(List.of(CPJudicialResult.builder()
                        .build()))
                    .build()))
            .id("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9")
            .pncId("20071234557L")
            .croNumber("croNumber")
            .build()
            .asDomain();

        assertThat(actual).isNull();
    }

    private static CPDefendant getCpDefendant() {
        return CPDefendant.builder()
            .isYouth(false)
            .personDefendant(CPPersonDefendant.builder()
                .personDetails(CPPersonDetails.builder()
                    .gender("MALE")
                    .title("title")
                    .firstName("firstname")
                    .middleName("middlename")
                    .lastName("lastname")
                    .dateOfBirth(LocalDate.of(2021, 1, 1))
                    .address(CPAddress.builder()
                        .address1("address1")
                        .build())
                    .contact(TEST_CP_CONTACT)
                    .build())
                .build())
            .offences(List.of(CPOffence.builder().id("1")
                    .offenceCode("ABC001")
                    .plea(Plea.builder().build())
                    .verdict(Verdict.builder().build())
                    .judicialResults(List.of(CPJudicialResult.builder()
                        .build()))
                    .build(),
                CPOffence.builder().id("2")
                    .offenceCode("ABC002")
                    .plea(Plea.builder().build())
                    .verdict(Verdict.builder().build())
                    .judicialResults(List.of(CPJudicialResult.builder()
                        .build()))
                    .build()))
            .id("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9")
            .pncId("20071234557L")
            .croNumber("croNumber")
            .build();
    }
}
