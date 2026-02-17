package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.JdbcTestBase;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.domain.vendor.VendorRule;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import com.mayureshpatel.pfdataservice.repository.vendor.VendorRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VendorRuleRepositoryTest extends JdbcTestBase {

    @Autowired
    private VendorRuleRepository vendorRuleRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("vendor_rule_test_user");
        user.setEmail("vendor_rule_test@example.com");
        user.setPasswordHash("hash");
        testUser = userRepository.insert(user);
    }

    private VendorRule buildRule(String keyword, String vendorName, User user) {
        VendorRule rule = new VendorRule();
        rule.setKeyword(keyword);
        rule.setVendorName(vendorName);
        rule.setPriority(1);
        rule.setUser(user);
        return rule;
    }

    @Test
    void insert_ShouldCreateVendorRule() {
        VendorRule saved = vendorRuleRepository.insert(buildRule("STARBUCKS", "Starbucks", testUser));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getKeyword()).isEqualTo("STARBUCKS");
    }

    @Test
    void findById_ShouldReturnVendorRule() {
        VendorRule saved = vendorRuleRepository.insert(buildRule("STARBUCKS", "Starbucks", testUser));

        Optional<VendorRule> found = vendorRuleRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getVendorName()).isEqualTo("Starbucks");
    }

    @Test
    void findByUserOrGlobal_ShouldReturnRules() {
        vendorRuleRepository.insert(buildRule("STARBUCKS", "Starbucks", testUser));
        vendorRuleRepository.insert(buildRule("GLOBAL_KWD", "Global Vendor", null));

        List<VendorRule> rules = vendorRuleRepository.findByUserOrGlobal(testUser.getId());

        assertThat(rules).hasSize(2);
    }

    @Test
    void update_ShouldUpdateRule() {
        VendorRule saved = vendorRuleRepository.insert(buildRule("OLD", "Old", testUser));
        saved.setKeyword("NEW");

        VendorRule updated = vendorRuleRepository.update(saved);

        assertThat(updated.getKeyword()).isEqualTo("NEW");
    }

    @Test
    void deleteById_ShouldRemoveRule() {
        VendorRule saved = vendorRuleRepository.insert(buildRule("TO_DELETE", "Delete", testUser));

        vendorRuleRepository.deleteById(saved.getId());

        assertThat(vendorRuleRepository.findById(saved.getId())).isEmpty();
    }
}
