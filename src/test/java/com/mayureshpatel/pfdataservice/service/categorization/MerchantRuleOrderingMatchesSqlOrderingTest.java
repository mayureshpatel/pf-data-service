package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves {@link RuleOrdering} reproduces {@code MerchantRuleQueries.FIND_ALL_BY_USER_ID}'s
 * ordering (priority desc, summed keyword length desc, id asc) against a real PostgreSQL
 * instance, so merchant rules rank in Java exactly as the SQL ranks them, the same guarantee
 * {@link RuleOrderingMatchesSqlOrderingTest} gives category rules.
 */
@Import(MerchantRuleRepository.class)
@DisplayName("RuleOrdering matches the merchant rule SQL ordering (PostgreSQL)")
class MerchantRuleOrderingMatchesSqlOrderingTest extends BaseRepositoryTest {

    private static final Long USER_1 = 1L;
    private static final Long MERCHANT_ID = 1L;

    @Autowired
    private MerchantRuleRepository repository;

    private void insert(int priority, String... keywords) {
        repository.insertAndReturnId(MerchantRule.builder()
                .keywords(List.of(keywords))
                .matchType(MatchType.OR)
                .priority(priority)
                .merchant(Merchant.builder().id(MERCHANT_ID).build())
                .user(User.builder().id(USER_1).build())
                .build());
    }

    @Test
    @DisplayName("ranking each saved rule as an edited draft over the shuffled saved set reproduces the sql order")
    void rank_reproducesFindByUserIdOrder() {
        // arrange -- full ties broken by id, multi-keyword sums, a lower id with a shorter sum,
        // and keywords whose character count differs from their utf-16 length
        insert(5, "BBBB");
        insert(5, "AAAA");
        insert(5, "CC", "DD");
        insert(5, "EEEEEEEEE");
        insert(5, "S");
        insert(3, "😀😀😀");
        insert(3, "ABCD");
        insert(3, "café");
        insert(0, "ZZ");
        insert(10, "X");
        insert(10, "LONGER KEYWORD");
        List<MerchantRule> sqlOrder = repository.findByUserId(USER_1);
        List<Long> expectedIds = sqlOrder.stream().map(MerchantRule::getId).toList();
        assertThat(expectedIds).hasSize(11);

        for (MerchantRule saved : sqlOrder) {
            List<MerchantRule> shuffled = new ArrayList<>(sqlOrder);
            Collections.shuffle(shuffled, new Random(saved.getId()));
            MerchantRule draft = saved.toBuilder().build();

            // act
            List<MerchantRule> ranked = RuleOrdering.rank(shuffled, draft, saved.getId());

            // assert & verify
            assertThat(ranked).extracting(MerchantRule::getId).containsExactlyElementsOf(expectedIds);
            assertThat(ranked).anySatisfy(rule -> assertThat(rule).isSameAs(draft));
        }
    }
}
