package uk.gov.justice.probation.courtcasematcher.restclient;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class NameHelper {

    public static final String ALL_CAPS_REGEX = "^[^a-z]*$";

    @Value("#{'${offender-search.ignore-titles}'.split(',')}")
    private final List<String> ignoreTitles;

    private String trimTitle(String name) {
        return Arrays.stream(name.split(" "))
            .filter(str -> !ignoreTitles.contains(str.toUpperCase()))
            .collect(Collectors.joining(" "));
    }

    public String getSurname(String fullName) {
        String name = trimTitle(fullName);
        return name
                .replace(getFirstName(name), "")
                .trim();
    }

    public String getFirstName(String fullName) {
        return Arrays.stream(trimTitle(fullName).split(" "))
                .takeWhile(s -> !s.matches(ALL_CAPS_REGEX))
                .collect(Collectors.joining(" "));
    }

    public Name getNameFromFields(String fullName) {

        String name = trimTitle(fullName);

        return Name.builder()
            .title(getTitle(fullName))
            .forename1(getFirstName(name))
            .surname(getSurname(name))
            .build();
    }

    private String getTitle(String fullName) {
        String[] words = fullName.split(" ");
        if (words.length >= 1 && words[0] != null) {
            return ignoreTitles.contains(words[0].toUpperCase()) ? words[0] : null;
        }
        return null;
    }
}
