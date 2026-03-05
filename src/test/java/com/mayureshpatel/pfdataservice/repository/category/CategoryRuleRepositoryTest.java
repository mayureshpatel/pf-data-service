//package com.mayureshpatel.pfdataservice.repository.category;
//
//import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
//import com.mayureshpatel.pfdataservice.domain.category.Category;
//import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
//import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
//import com.mayureshpatel.pfdataservice.domain.user.User;
//import com.mayureshpatel.pfdataservice.util.TestDataFactory;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.concurrent.atomic.AtomicLong;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@Transactional
//@DisplayName("CategoryRuleRepository Integration Tests")
//class CategoryRuleRepositoryTest extends BaseIntegrationTest {
//
//    @Autowired
//    private CategoryRuleRepository categoryRuleRepository;
//
//    @Autowired
//    private TestDataFactory factory;
//
//    private User testUser;
//    private Category testCategory;
//
//    // CategoryRule INSERT uses ON CONFLICT (id) — requires explicit ID
//    private static final AtomicLong ruleIdSequence = new AtomicLong(100000);
//
//    @BeforeEach
//    void setUp() {
//        testUser = factory.createUser("rule_" + System.currentTimeMillis());
//        testCategory = factory.createCategory(testUser, "Groceries", CategoryType.EXPENSE);
//    }
//
//    private CategoryRule createRule(String keyword, int priority) {
//        CategoryRule rule = new CategoryRule();
//        rule.setId(ruleIdSequence.incrementAndGet());
//        rule.setUser(testUser);
//        rule.setCategory(testCategory);
//        rule.setKeyword(keyword);
//        rule.setPriority(priority);
//        rule.setAudit(new TimestampAudit());
//        return categoryRuleRepository.save(rule);
//    }
//
//    @Test
//    @DisplayName("save() should persist new category rule")
//    void save_shouldPersistRule() {
//        CategoryRule rule = createRule("walmart", 1);
//
//        assertThat(rule).isNotNull();
//        assertThat(rule.getKeyword()).isEqualTo("walmart");
//    }
//
//    @Test
//    @DisplayName("findByUserId() should return all rules for user")
//    void findByUserId_shouldReturnUserRules() {
//        createRule("walmart", 1);
//        createRule("kroger", 2);
//
//        List<CategoryRule> rules = categoryRuleRepository.findByUserId(testUser.getId());
//
//        assertThat(rules).hasSize(2);
//        assertThat(rules).extracting(CategoryRule::getKeyword).containsExactlyInAnyOrder("walmart", "kroger");
//    }
//
//    @Test
//    @DisplayName("findByUserId() should return empty when user has no rules")
//    void findByUserId_shouldReturnEmpty() {
//        User otherUser = factory.createUser("norules_" + System.currentTimeMillis());
//
//        List<CategoryRule> rules = categoryRuleRepository.findByUserId(otherUser.getId());
//
//        assertThat(rules).isEmpty();
//    }
//
//    @Test
//    @DisplayName("deleteById() should remove rule")
//    void deleteById_shouldRemoveRule() {
//        CategoryRule rule = createRule("target", 1);
//
//        categoryRuleRepository.deleteById(rule.getId());
//
//        List<CategoryRule> remaining = categoryRuleRepository.findByUserId(testUser.getId());
//        assertThat(remaining).isEmpty();
//    }
//
//    @Test
//    @DisplayName("save() should update existing rule (upsert)")
//    void save_shouldUpdateExistingRule() {
//        CategoryRule rule = createRule("amazon", 1);
//
//        rule.setKeyword("amazon prime");
//        rule.setPriority(5);
//        categoryRuleRepository.save(rule);
//
//        List<CategoryRule> rules = categoryRuleRepository.findByUserId(testUser.getId());
//        assertThat(rules).hasSize(1);
//        assertThat(rules.get(0).getKeyword()).isEqualTo("amazon prime");
//        assertThat(rules.get(0).getPriority()).isEqualTo(5);
//    }
//
//    @Test
//    @DisplayName("data isolation - user cannot see other user's rules")
//    void dataIsolation_shouldIsolateByUser() {
//        createRule("user1rule", 1);
//
//        User otherUser = factory.createUser("otherrule_" + System.currentTimeMillis());
//        Category otherCat = factory.createCategory(otherUser, "OtherCat", CategoryType.EXPENSE);
//
//        CategoryRule otherRule = new CategoryRule();
//        otherRule.setId(ruleIdSequence.incrementAndGet());
//        otherRule.setUser(otherUser);
//        otherRule.setCategory(otherCat);
//        otherRule.setKeyword("user2rule");
//        otherRule.setPriority(1);
//        otherRule.setAudit(new TimestampAudit());
//        categoryRuleRepository.save(otherRule);
//
//        List<CategoryRule> user1Rules = categoryRuleRepository.findByUserId(testUser.getId());
//        List<CategoryRule> user2Rules = categoryRuleRepository.findByUserId(otherUser.getId());
//
//        assertThat(user1Rules).extracting(CategoryRule::getKeyword).containsExactly("user1rule");
//        assertThat(user2Rules).extracting(CategoryRule::getKeyword).containsExactly("user2rule");
//    }
//}
