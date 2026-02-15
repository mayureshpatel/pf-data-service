package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.repository.vendor.model.VendorRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class VendorCleanerTest {

    @Mock
    private VendorRuleRepository vendorRuleRepository;

    private VendorCleaner vendorCleaner;
    private List<VendorRule> rules;

    @BeforeEach
    void setUp() {
        vendorCleaner = new VendorCleaner(vendorRuleRepository);

        // Mock rules - Order matters (simulating Priority DESC, Length DESC)
        rules = List.of(
                new VendorRule(1L, "AMAZON.COM", "Amazon", 10, null, null, null),
                new VendorRule(2L, "AMAZON MKTPLACE", "Amazon Marketplace", 5, null, null, null),
                new VendorRule(3L, "AMAZON", "Amazon", 1, null, null, null),
                new VendorRule(4L, "SHELL OIL", "Shell", 5, null, null, null),
                new VendorRule(5L, "SHELL", "Shell", 1, null, null, null),
                new VendorRule(6L, "UBER EATS", "Uber Eats", 5, null, null, null),
                new VendorRule(7L, "UBER", "Uber", 1, null, null, null)
        );
    }

    @Test
    void shouldCleanKnownVendors() {
        assertThat(vendorCleaner.cleanVendorName("AMAZON.COM*1234", rules)).isEqualTo("Amazon");
        assertThat(vendorCleaner.cleanVendorName("AMAZON MKTPLACE PAYMENTS", rules)).isEqualTo("Amazon Marketplace");
        assertThat(vendorCleaner.cleanVendorName("AMAZON WEB SERVICES", rules)).isEqualTo("Amazon"); // Matches "AMAZON"
        assertThat(vendorCleaner.cleanVendorName("Shell Oil 123", rules)).isEqualTo("Shell");
        assertThat(vendorCleaner.cleanVendorName("Uber Eats Pending", rules)).isEqualTo("Uber Eats");
        assertThat(vendorCleaner.cleanVendorName("Uber Trip", rules)).isEqualTo("Uber");
    }

    @Test
    void shouldReturnNullForUnknown() {
        assertThat(vendorCleaner.cleanVendorName("Mystery Shop", rules)).isNull();
        assertThat(vendorCleaner.cleanVendorName("Check #123", rules)).isNull();
    }

    @Test
    void shouldHandleNullDescription() {
        assertThat(vendorCleaner.cleanVendorName(null, rules)).isNull();
    }
}
