package uk.gov.justice.probation.courtcasematcher.restclient;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class NameHelper {
    public static final String ALL_CAPS_REGEX = "^[^a-z]*$";
    public static String getSurname(String fullName) {
        return fullName
                .replace(getFirstName(fullName), "")
                .trim();
    }

    public static String getFirstName(String fullName) {
        return Arrays.stream(fullName.split(" "))
                .takeWhile(s -> !s.matches(ALL_CAPS_REGEX))
                .collect(Collectors.joining(" "));
    }
}
