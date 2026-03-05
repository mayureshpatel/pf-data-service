//package com.mayureshpatel.pfdataservice.service.transfer;
//
//import com.mayureshpatel.pfdataservice.domain.account.Account;
//import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
//import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
//import com.mayureshpatel.pfdataservice.domain.transaction.TransactionType;
//import com.mayureshpatel.pfdataservice.domain.user.User;
//import com.mayureshpatel.pfdataservice.dto.transaction.TransferSuggestionDto;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.math.BigDecimal;
//import java.time.OffsetDateTime;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DisplayName("TransferMatcher unit tests")
//class TransferMatcherTest {
//
//    private final TransferMatcher transferMatcher = new TransferMatcher();
//
//    private User buildUser(Long id) {
//        User user = new User();
//        user.setId(id);
//        return user;
//    }
//
//    private Account buildAccount(Long id, User user, BigDecimal balance) {
//        Account account = new Account();
//        account.setId(id);
//        account.setUser(user);
//        account.setCurrentBalance(balance);
//        return account;
//    }
//
//    private Transaction buildTransaction(Long id, Account account, BigDecimal amount,
//                                         TransactionType type, OffsetDateTime date) {
//        Transaction t = new Transaction();
//        t.setId(id);
//        t.setAccount(account);
//        t.setAmount(amount);
//        t.setType(type);
//        t.setTransactionDate(date);
//        t.setMerchant(new Merchant()); // Required for TransactionDtoMapper
//        return t;
//    }
//
//    @Test
//    @DisplayName("should return empty list when no transactions exist")
//    void findMatches_noTransactions_returnsEmptyList() {
//        List<TransferSuggestionDto> result = transferMatcher.findMatches(List.of());
//        assertThat(result).isEmpty();
//    }
//
//    @Test
//    @DisplayName("should detect a transfer pair when amounts match, types differ, accounts differ, and within 3 days")
//    void findMatches_matchingPair_returnsSuggestion() {
//        User user = buildUser(1L);
//        Account accountA = buildAccount(1L, user, BigDecimal.ZERO);
//        Account accountB = buildAccount(2L, user, BigDecimal.ZERO);
//        OffsetDateTime now = OffsetDateTime.now();
//
//        Transaction t1 = buildTransaction(1L, accountA, new BigDecimal("100"), TransactionType.INCOME, now);
//        Transaction t2 = buildTransaction(2L, accountB, new BigDecimal("100"), TransactionType.EXPENSE, now.plusDays(1));
//
//        List<TransferSuggestionDto> result = transferMatcher.findMatches(List.of(t1, t2));
//
//        assertThat(result).hasSize(1);
//        // confidence = 0.9 - (1 * 0.1) = 0.8
//        assertThat(result.get(0).confidenceScore()).isCloseTo(0.8, org.assertj.core.data.Offset.offset(1e-9));
//    }
//
//    @Test
//    @DisplayName("should not suggest transfer when transactions are in the same account")
//    void findMatches_sameAccount_noSuggestion() {
//        User user = buildUser(1L);
//        Account account = buildAccount(1L, user, BigDecimal.ZERO);
//        OffsetDateTime now = OffsetDateTime.now();
//
//        Transaction t1 = buildTransaction(1L, account, new BigDecimal("100"), TransactionType.INCOME, now);
//        Transaction t2 = buildTransaction(2L, account, new BigDecimal("100"), TransactionType.EXPENSE, now);
//
//        List<TransferSuggestionDto> result = transferMatcher.findMatches(List.of(t1, t2));
//
//        assertThat(result).isEmpty();
//    }
//
//    @Test
//    @DisplayName("should not suggest transfer when transaction types are the same")
//    void findMatches_sameType_noSuggestion() {
//        User user = buildUser(1L);
//        Account accountA = buildAccount(1L, user, BigDecimal.ZERO);
//        Account accountB = buildAccount(2L, user, BigDecimal.ZERO);
//        OffsetDateTime now = OffsetDateTime.now();
//
//        Transaction t1 = buildTransaction(1L, accountA, new BigDecimal("100"), TransactionType.EXPENSE, now);
//        Transaction t2 = buildTransaction(2L, accountB, new BigDecimal("100"), TransactionType.EXPENSE, now);
//
//        List<TransferSuggestionDto> result = transferMatcher.findMatches(List.of(t1, t2));
//
//        assertThat(result).isEmpty();
//    }
//
//    @Test
//    @DisplayName("should not suggest transfer when amounts differ")
//    void findMatches_differentAmounts_noSuggestion() {
//        User user = buildUser(1L);
//        Account accountA = buildAccount(1L, user, BigDecimal.ZERO);
//        Account accountB = buildAccount(2L, user, BigDecimal.ZERO);
//        OffsetDateTime now = OffsetDateTime.now();
//
//        Transaction t1 = buildTransaction(1L, accountA, new BigDecimal("100"), TransactionType.INCOME, now);
//        Transaction t2 = buildTransaction(2L, accountB, new BigDecimal("200"), TransactionType.EXPENSE, now);
//
//        List<TransferSuggestionDto> result = transferMatcher.findMatches(List.of(t1, t2));
//
//        assertThat(result).isEmpty();
//    }
//
//    @Test
//    @DisplayName("should use confidence score of 0.9 when transactions occur on the same day")
//    void findMatches_sameDayTransactions_confidenceIsMax() {
//        User user = buildUser(1L);
//        Account accountA = buildAccount(1L, user, BigDecimal.ZERO);
//        Account accountB = buildAccount(2L, user, BigDecimal.ZERO);
//        OffsetDateTime now = OffsetDateTime.now();
//
//        Transaction t1 = buildTransaction(1L, accountA, new BigDecimal("50"), TransactionType.INCOME, now);
//        Transaction t2 = buildTransaction(2L, accountB, new BigDecimal("50"), TransactionType.EXPENSE, now);
//
//        List<TransferSuggestionDto> result = transferMatcher.findMatches(List.of(t1, t2));
//
//        assertThat(result).hasSize(1);
//        assertThat(result.get(0).confidenceScore()).isEqualTo(0.9);
//    }
//}
