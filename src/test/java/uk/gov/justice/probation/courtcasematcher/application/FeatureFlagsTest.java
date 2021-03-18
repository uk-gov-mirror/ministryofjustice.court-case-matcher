package uk.gov.justice.probation.courtcasematcher.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureFlagsTest {

    private final FeatureFlags featureFlags = new FeatureFlags();

    @DisplayName("Set and get toggle value")
    @Test
    void testGetSimpleFlag() {
        featureFlags.setFlagValue("flag-test", false);

        assertThat(featureFlags.getFlags().get("flag-test")).isFalse();
    }
}
