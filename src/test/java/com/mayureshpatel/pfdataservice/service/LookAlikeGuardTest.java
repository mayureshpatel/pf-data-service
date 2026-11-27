package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.dto.merchant.LookAlikeWarningDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link LookAlikeGuard}: the Kroger / Kroger Fuel Center case is flagged through the
 * rival merchant and the look-alike group, while descriptions that fit the merchant, or carry extra
 * words nothing else claims, draw no warning.
 */
@DisplayName("LookAlikeGuard Unit Tests")
class LookAlikeGuardTest {

    private static final Merchant KROGER = Merchant.builder().id(1L).name("Kroger").build();
    private static final Merchant FUEL_CENTER = Merchant.builder().id(2L).name("Kroger Fuel Center").build();
    private static final Merchant WALMART = Merchant.builder().id(3L).name("Walmart").build();
    private static final String STORE_431 = "KROGER #431 DAYTON OH";
    private static final String FUEL_1 = "KROGER FUEL CTR #1 DAYTON OH";

    @Test
    @DisplayName("should flag fuel descriptions linked to Kroger, naming Kroger Fuel Center and the plain group")
    void shouldFlagFuelDescriptionsForKroger() {
        // arrange
        List<String> descriptions = List.of(
                STORE_431,
                FUEL_1,
                "KROGER #122 CENTERVILLE OH",
                "KROGER FUEL CTR #2 KETTERING OH");

        // act
        List<LookAlikeWarningDto> warnings = LookAlikeGuard.check(KROGER, List.of(KROGER, FUEL_CENTER, WALMART), descriptions);

        // assert & verify
        assertThat(warnings).hasSize(1);
        LookAlikeWarningDto warning = warnings.get(0);
        assertThat(warning.groupName()).isEqualTo("KROGER FUEL CTR");
        assertThat(warning.words()).containsExactly("FUEL", "CTR");
        assertThat(warning.descriptions()).containsExactly(FUEL_1, "KROGER FUEL CTR #2 KETTERING OH");
        assertThat(warning.merchants()).extracting(MerchantDto::name).containsExactly("Kroger Fuel Center");
        assertThat(warning.nextToGroup()).isEqualTo("KROGER");
    }

    @Test
    @DisplayName("should not warn when every description fits the merchant, even with a look-alike merchant around")
    void shouldNotWarnWhenDescriptionsFit() {
        // arrange
        List<String> descriptions = List.of(STORE_431, "KROGER #122 CENTERVILLE OH");

        // act
        List<LookAlikeWarningDto> toKroger = LookAlikeGuard.check(KROGER, List.of(KROGER, FUEL_CENTER), descriptions);
        List<LookAlikeWarningDto> fuelToFuel = LookAlikeGuard.check(
                FUEL_CENTER, List.of(KROGER, FUEL_CENTER), List.of(FUEL_1));

        // assert & verify
        assertThat(toKroger).isEmpty();
        assertThat(fuelToFuel).isEmpty();
    }

    @Test
    @DisplayName("should not warn about extra words that no merchant or selected group claims")
    void shouldNotWarnAboutUnclaimedWords() {
        // arrange
        Merchant amazon = Merchant.builder().id(4L).name("Amazon").build();
        Merchant payments = Merchant.builder().id(5L).name("Payments Co").build();

        // act
        List<LookAlikeWarningDto> marketplace = LookAlikeGuard.check(
                KROGER, List.of(KROGER, WALMART), List.of("KROGER MARKETPLACE #12 DAYTON OH"));
        List<LookAlikeWarningDto> weakerRival = LookAlikeGuard.check(
                amazon, List.of(amazon, payments), List.of("AMAZON MKTPLACE PMTS"));

        // assert & verify
        assertThat(marketplace).isEmpty();
        assertThat(weakerRival).isEmpty();
    }

    @Test
    @DisplayName("should flag a look-alike group in the selection even when no other merchant exists yet")
    void shouldFlagLookAlikeGroupWithoutRival() {
        // arrange
        List<String> descriptions = List.of(STORE_431, FUEL_1);

        // act
        List<LookAlikeWarningDto> warnings = LookAlikeGuard.check(KROGER, List.of(KROGER), descriptions);
        List<LookAlikeWarningDto> fuelAlone = LookAlikeGuard.check(KROGER, List.of(KROGER), List.of(FUEL_1));

        // assert & verify
        assertThat(warnings).singleElement().satisfies(warning -> {
            assertThat(warning.words()).containsExactly("FUEL", "CTR");
            assertThat(warning.merchants()).isEmpty();
            assertThat(warning.nextToGroup()).isEqualTo("KROGER");
        });
        assertThat(fuelAlone).isEmpty();
    }

    @Test
    @DisplayName("should not warn when the merchant's name has words the descriptions lack (only extra description words count)")
    void shouldNotWarnAboutWordsOnlyTheMerchantHas() {
        // arrange
        List<String> descriptions = List.of(STORE_431);

        // act
        List<LookAlikeWarningDto> warnings = LookAlikeGuard.check(FUEL_CENTER, List.of(KROGER, FUEL_CENTER), descriptions);

        // assert & verify
        assertThat(warnings).isEmpty();
    }

    @Test
    @DisplayName("should list warnings with the most descriptions first")
    void shouldOrderWarningsByDescriptionCount() {
        // arrange
        Merchant pharmacy = Merchant.builder().id(6L).name("Kroger Pharmacy").build();
        List<String> descriptions = List.of(
                "KROGER PHARMACY #9",
                "KROGER FUEL CTR #1",
                "KROGER FUEL CTR #2");

        // act
        List<LookAlikeWarningDto> warnings = LookAlikeGuard.check(KROGER, List.of(KROGER, FUEL_CENTER, pharmacy), descriptions);

        // assert & verify
        assertThat(warnings).extracting(LookAlikeWarningDto::groupName).containsExactly("KROGER FUEL CTR", "KROGER PHARMACY");
        assertThat(warnings.get(1).merchants()).extracting(MerchantDto::name).containsExactly("Kroger Pharmacy");
        assertThat(warnings.get(1).nextToGroup()).isNull();
    }
}
