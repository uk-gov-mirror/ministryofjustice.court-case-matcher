package uk.gov.justice.probation.courtcasematcher.messaging;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.probation.courtcasematcher.application.MessagingConfig;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPAddress;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPCourtCentre;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPDefendant;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearing;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingDay;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingEvent;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPJurisdictionType;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPLegalEntityDefendant;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPOffence;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPOrganisation;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPPersonDefendant;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPPersonDetails;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPProsecutionCase;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.ProsecutionCaseIdentifier;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraAddress;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraHearing;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraName;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraOffence;
import uk.gov.justice.probation.courtcasematcher.model.SnsMessageContainer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.justice.probation.courtcasematcher.messaging.model.MessageType.COMMON_PLATFORM_HEARING;

@ExtendWith(SpringExtension.class)
@DisplayName("Message Parser Test")
@Profile("test")
class MessageParserTest {
    @Nested
    @DisplayName("SNS Message Container Json")
    @Import(MessagingConfig.class)
    class SnsMessageContainerJsonMessageParser {

        @Autowired
        @Qualifier("snsMessageWrapperJsonParser")
        public MessageParser<SnsMessageContainer> messageParser;

        @Test
        void whenValidContainer_ThenReturn() throws IOException {
            var path = "src/test/resources/messages/sns-message-container.json";
            var content = Files.readString(Paths.get(path));

            var messageContainer = messageParser.parseMessage(content, SnsMessageContainer.class);

            assertThat(messageContainer).isNotNull();
            assertThat(messageContainer.getMessageAttributes().getMessageType()).isEqualTo(COMMON_PLATFORM_HEARING);
            assertThat(messageContainer.getMessageAttributes().getHearingEventType().getValue()).isEqualTo("Resulted");
        }
    }

    @Nested
    @DisplayName("Common Platform Json")
    @Import(MessagingConfig.class)
    class CommonPlatformJsonMessageParser {

        @Autowired
        @Qualifier("commonPlatformJsonParser")
        public MessageParser<CPHearingEvent> messageParser;

        @Test
        void whenValidCase_ThenReturn() throws IOException {
            var path = "src/test/resources/messages/common-platform/hearing.json";
            var content = Files.readString(Paths.get(path));

            var aHearingEvent = messageParser.parseMessage(content, CPHearingEvent.class);

            final var defendants = List.of(defendant1(), defendant2());
            checkHearing(aHearingEvent.getHearing(), defendants);
        }

        @Test
        void givenLegalEntityDefendant_whenValidCase_ThenReturn() throws IOException {
            var path = "src/test/resources/messages/common-platform/hearing-with-legal-entity-defendant.json";
            var content = Files.readString(Paths.get(path));

            var aHearingEvent = messageParser.parseMessage(content, CPHearingEvent.class);
            checkHearing(aHearingEvent.getHearing(), List.of(defendant1(), legalEntityDefendant()));
        }

        @Test
        void whenInvalidCase_ThenThrow() throws IOException {
            var path = "src/test/resources/messages/common-platform/hearing-invalid.json";
            var content = Files.readString(Paths.get(path));

            var thrown = catchThrowable(() -> messageParser.parseMessage(content, CPHearingEvent.class));

            var ex = (ConstraintViolationException) thrown;
            assertThat(ex.getConstraintViolations()).hasSize(9);
            assertThat(ex.getConstraintViolations()).anyMatch(validationError("hearing.courtCentre.roomName", "must not be blank"));
            assertThat(ex.getConstraintViolations()).anyMatch(validationError("hearing.hearingDays[0].sittingDay", "must not be null"));
            assertThat(ex.getConstraintViolations()).anyMatch(validationError("hearing.jurisdictionType", "must not be null"));
            assertThat(ex.getConstraintViolations()).anyMatch(validationError("hearing.prosecutionCases[0].id", "must not be blank"));
            assertThat(ex.getConstraintViolations()).anyMatch(validationError("hearing.prosecutionCases[0].defendants[0].id", "must not be blank"));
            assertThat(ex.getConstraintViolations()).anyMatch(validationError("hearing.prosecutionCases[0].defendants[0].offences[0].wording", "must not be blank"));
            assertThat(ex.getConstraintViolations()).anyMatch(validationError("hearing.prosecutionCases[0].defendants[0].personDefendant.personDetails.lastName", "must not be blank"));
            assertThat(ex.getConstraintViolations()).anyMatch(validationError("hearing.prosecutionCases[0].defendants[0].personDefendant.personDetails.address.address1", "must not be blank"));
            assertThat(ex.getConstraintViolations()).anyMatch(validationError("hearing.prosecutionCases[0].defendants[1].legalEntityDefendant.organisation.name", "must not be blank"));
        }

        private Predicate<ConstraintViolation<?>> validationError(String path, String message) {
            return cv -> cv.getMessage().equals(message)
                    && cv.getPropertyPath().toString().equals(path);
        }

        private void checkHearing(CPHearing actual, List<CPDefendant> defendants) {
            assertThat(actual).isNotNull();
            assertThat(actual.getId()).isEqualTo("E10E3EF3-8637-40E3-BDED-8ED104A380AC");
            assertThat(actual.getCourtCentre()).isEqualTo(
                    CPCourtCentre.builder()
                            .id("9b583616-049b-30f9-a14f-028a53b7cfe8")
                            .roomId("7cb09222-49e1-3622-a5a6-ad253d2b3c39")
                            .roomName("Crown Court 3-1")
                            .code("B10JQ00")
                            .build()
            );
            assertThat(actual.getHearingDays()).containsExactly(
                    CPHearingDay.builder()
                            .listedDurationMinutes(60)
                            .listingSequence(0)
                            .sittingDay(LocalDateTime.of(2021, 9, 8, 9, 0))
                            .build(),
                    CPHearingDay.builder()
                            .listedDurationMinutes(30)
                            .listingSequence(1)
                            .sittingDay(LocalDateTime.of(2021, 9, 9, 10, 30))
                            .build()
            );
            assertThat(actual.getProsecutionCases()).hasSize(1);
            assertThat(actual.getProsecutionCases().get(0))
                    .usingRecursiveComparison()
                    .isEqualTo(CPProsecutionCase.builder()
                            .id("D2B61C8A-0684-4764-B401-F0A788BC7CCF")
                            .defendants(defendants)
                            .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseUrn("25GD34377719").build())
                            .build());
            assertThat(actual.getJurisdictionType()).isEqualTo(CPJurisdictionType.CROWN);
        }
        private CPDefendant legalEntityDefendant() {
            return CPDefendant.builder()
                    .id("903c4c54-f667-4770-8fdf-1adbb5957c25")
                    .prosecutionCaseId("D2B61C8A-0684-4764-B401-F0A788BC7CCF")
                    .pncId(null)
                    .croNumber(null)
                    .offences(List.of(CPOffence.builder()
                                    .id("50474F6F-65FC-48C7-AA83-16277B55B3BA")
                                    .offenceLegislation("Contrary to section 20 of the Offences Against the    Person Act 1861.")
                                    .offenceTitle("Wound / inflict grievous bodily harm without intent")
                                    .wording("on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith")
                                    .offenceCode("ABC001")
                                    .build(),
                            CPOffence.builder()
                                    .id("7103C6BD-5805-4EF8-B524-D34B9ADD43D1")
                                    .offenceLegislation("Contrary to section 20 of the Offences Against the    Person Act 1861.")
                                    .offenceTitle("Wound / inflict grievous bodily harm without intent")
                                    .wording("on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, Jane Smith")
                                    .offenceCode("ABC002")
                                    .build()))
                    .personDefendant(null)
                    .legalEntityDefendant(CPLegalEntityDefendant.builder()
                            .organisation(CPOrganisation.builder()
                                    .name("An Organisation")
                                    .build())
                            .build())
                    .build();
        }

        private CPDefendant defendant1() {
            return CPDefendant.builder()
                    .id("f91ef118-a51f-4874-9409-c0538b4ca6fd")
                    .prosecutionCaseId("D2B61C8A-0684-4764-B401-F0A788BC7CCF")
                    .pncId("2004/0012345U")
                    .croNumber("12345ABCDEF")
                    .offences(List.of(CPOffence.builder()
                                    .id("a63d9020-aa6b-4997-92fd-72a692b036de")
                                    .offenceLegislation("Contrary to section 20 of the Offences Against the    Person Act 1861.")
                                    .offenceTitle("Wound / inflict grievous bodily harm without intent")
                                    .wording("on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith")
                                    .listingNumber(30)
                                    .offenceCode("ABC001")
                                    .build(),
                            CPOffence.builder()
                                    .id("ea1c2cf1-f155-483b-a908-81158a9b2f9b")
                                    .offenceLegislation("Contrary to section 20 of the Offences Against the    Person Act 1861.")
                                    .offenceTitle("Wound / inflict grievous bodily harm without intent")
                                    .wording("on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, Jane Smith")
                                    .listingNumber(20)
                                    .offenceCode("ABC002")
                                    .build()))
                    .personDefendant(CPPersonDefendant.builder()
                            .personDetails(CPPersonDetails.builder()
                                    .address(CPAddress.builder()
                                            .address1("13 Wind Street")
                                            .address2("Swansea")
                                            .address3("Wales")
                                            .address4("UK")
                                            .address5("Earth")
                                            .postcode("SA1 1FU")
                                            .build())
                                    .dateOfBirth(LocalDate.of(1975, 1, 1))
                                    .title("Mr")
                                    .firstName("Arthur")
                                    .middleName(null)
                                    .lastName("MORGAN")
                                    .gender("MALE")
                                    .build())
                            .build())
                    .build();
        }

        private CPDefendant defendant2() {
            return CPDefendant.builder()
                    .id("903c4c54-f667-4770-8fdf-1adbb5957c25")
                    .prosecutionCaseId("D2B61C8A-0684-4764-B401-F0A788BC7CCF")
                    .pncId(null)
                    .croNumber(null)
                    .offences(List.of(CPOffence.builder()
                                    .id("1391ADC2-7A43-48DC-8523-3D28B9DCD2B7")
                                    .offenceLegislation("Contrary to section 20 of the Offences Against the    Person Act 1861.")
                                    .offenceTitle("Wound / inflict grievous bodily harm without intent")
                                    .wording("on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith")
                                    .offenceCode("ABC001")
                                    .build(),
                            CPOffence.builder()
                                    .id("19C08FB0-363B-4EB1-938D-76EF751E5D66")
                                    .offenceLegislation("Contrary to section 20 of the Offences Against the    Person Act 1861.")
                                    .offenceTitle("Wound / inflict grievous bodily harm without intent")
                                    .wording("on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, Jane Smith")
                                    .offenceCode("ABC002")
                                    .build()))
                    .personDefendant(CPPersonDefendant.builder()
                            .personDetails(CPPersonDetails.builder()
                                    .address(CPAddress.builder()
                                            .address1("14 Tottenham Court Road")
                                            .address2("London Road")
                                            .address3("England")
                                            .address4("UK")
                                            .address5("Earth")
                                            .postcode("W1T 7RJ")
                                            .build())
                                    .dateOfBirth(LocalDate.of(1997, 2, 28))
                                    .title(null)
                                    .firstName("Phyllis")
                                    .middleName("Ulon")
                                    .lastName("Leffler")
                                    .gender("FEMALE")
                                    .build())
                            .build())
                    .build();
        }

    }

    @Nested
    @DisplayName("Libra JSON")
    @Import(MessagingConfig.class)
    class LibraJsonMessageParser {

        @Autowired
        @Qualifier("libraJsonParser")
        private MessageParser<LibraHearing> messageParser;

        @DisplayName("Parse a valid message")
        @Test
        void whenValidCase_ThenReturn() throws IOException {
            var path = "src/test/resources/messages/libra/case.json";
            var content = Files.readString(Paths.get(path));

            var aCase = messageParser.parseMessage(content, LibraHearing.class);
            checkLibraCase(aCase);
        }

        @DisplayName("Parse an invalid message")
        @Test
        void whenInvalidCase_ThenThrowConstraintViolation() throws IOException {
            var path = "src/test/resources/messages/libra/case-invalid.json";
            var content = Files.readString(Paths.get(path));

            var thrown = catchThrowable(() -> messageParser.parseMessage(content, LibraHearing.class));

            var ex = (ConstraintViolationException) thrown;
            assertThat(ex.getConstraintViolations()).hasSize(1);
            assertThat(ex.getConstraintViolations()).anyMatch(cv -> cv.getMessage().equals("must not be blank")
                    && cv.getPropertyPath().toString().equals("caseNo"));
        }
    }

    private static void checkLibraCase(LibraHearing aLibraHearing) {
        // Fields populated from the session
        assertThat(aLibraHearing.getDefendantAge()).isEqualTo("20");
        assertThat(aLibraHearing.getDefendantName()).isEqualTo("Mr Arthur MORGAN");
        assertThat(aLibraHearing.getName()).isEqualTo(LibraName.builder()
                .title("Mr")
                .forename1("Arthur")
                .surname("MORGAN").build());
        assertThat(aLibraHearing.getDefendantType()).isEqualTo("P");
        assertThat(aLibraHearing.getDefendantSex()).isEqualTo("N");
        assertThat(aLibraHearing.getDefendantAge()).isEqualTo("20");
        assertThat(aLibraHearing.getPnc()).isEqualTo("2004/0012345U");
        assertThat(aLibraHearing.getCro()).isEqualTo("11111/79J");
        assertThat(aLibraHearing.getDefendantAddress()).usingRecursiveComparison().isEqualTo(LibraAddress.builder()
                .line1("39 The Street")
                .line2("Newtown")
                .pcode("NT4 6YH").build());
        assertThat(aLibraHearing.getDefendantDob()).isEqualTo(LocalDate.of(1975, Month.JANUARY, 1));
        assertThat(aLibraHearing.getNationality1()).isEqualTo("Angolan");
        assertThat(aLibraHearing.getNationality2()).isEqualTo("Austrian");
        assertThat(aLibraHearing.getSeq()).isEqualTo(1);
        assertThat(aLibraHearing.getListNo()).isEqualTo("1st");
        assertThat(aLibraHearing.getOffences()).hasSize(1);
        checkOffence(aLibraHearing.getOffences().get(0));
    }

    private static void checkOffence(LibraOffence offence) {
        assertThat(offence.getSeq()).isEqualTo(1);
        assertThat(offence.getTitle()).isEqualTo("Theft from a shop");
        assertThat(offence.getSummary()).isEqualTo("On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Person.");
        assertThat(offence.getAct()).isEqualTo("Contrary to section 1(1) and 7 of the Theft Act 1968.");
    }

}
