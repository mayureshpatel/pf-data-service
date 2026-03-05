//package com.mayureshpatel.pfdataservice.security;
//
//import com.mayureshpatel.pfdataservice.domain.account.Account;
//import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
//import com.mayureshpatel.pfdataservice.domain.user.User;
//import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
//import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("SecurityService unit tests")
//class SecurityServiceTest {
//
//    @Mock
//    private AccountRepository accountRepository;
//
//    @Mock
//    private TransactionRepository transactionRepository;
//
//    @InjectMocks
//    private SecurityService securityService;
//
//    private static final Long USER_ID = 1L;
//    private static final Long ACCOUNT_ID = 10L;
//    private static final Long TRANSACTION_ID = 100L;
//
//    private User buildUser(Long id) {
//        User user = new User();
//        user.setId(id);
//        user.setUsername("testuser");
//        user.setEmail("test@example.com");
//        user.setPasswordHash("hash");
//        return user;
//    }
//
//    private CustomUserDetails buildUserDetails(Long userId) {
//        return new CustomUserDetails(buildUser(userId));
//    }
//
//    private Account buildAccountOwnedBy(Long userId) {
//        User user = buildUser(userId);
//        Account account = new Account();
//        account.setId(ACCOUNT_ID);
//        account.setUser(user);
//        return account;
//    }
//
//    private Transaction buildTransactionOwnedBy(Long userId) {
//        Account account = buildAccountOwnedBy(userId);
//        Transaction transaction = new Transaction();
//        transaction.setId(TRANSACTION_ID);
//        transaction.setAccount(account);
//        return transaction;
//    }
//
//    @Nested
//    @DisplayName("isAccountOwner")
//    class IsAccountOwnerTest {
//
//        @Test
//        @DisplayName("should return true when user owns the account")
//        void isAccountOwner_userOwnsAccount_returnsTrue() {
//            CustomUserDetails userDetails = buildUserDetails(USER_ID);
//            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(buildAccountOwnedBy(USER_ID)));
//
//            assertThat(securityService.isAccountOwner(ACCOUNT_ID, userDetails)).isTrue();
//        }
//
//        @Test
//        @DisplayName("should return false when user does not own the account")
//        void isAccountOwner_differentUser_returnsFalse() {
//            CustomUserDetails userDetails = buildUserDetails(USER_ID);
//            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(buildAccountOwnedBy(999L)));
//
//            assertThat(securityService.isAccountOwner(ACCOUNT_ID, userDetails)).isFalse();
//        }
//
//        @Test
//        @DisplayName("should return false when account is not found")
//        void isAccountOwner_accountNotFound_returnsFalse() {
//            CustomUserDetails userDetails = buildUserDetails(USER_ID);
//            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
//
//            assertThat(securityService.isAccountOwner(ACCOUNT_ID, userDetails)).isFalse();
//        }
//
//        @Test
//        @DisplayName("should return false when accountId is null")
//        void isAccountOwner_nullAccountId_returnsFalse() {
//            CustomUserDetails userDetails = buildUserDetails(USER_ID);
//
//            assertThat(securityService.isAccountOwner(null, userDetails)).isFalse();
//        }
//
//        @Test
//        @DisplayName("should return false when userDetails is null")
//        void isAccountOwner_nullUserDetails_returnsFalse() {
//            assertThat(securityService.isAccountOwner(ACCOUNT_ID, null)).isFalse();
//        }
//    }
//
//    @Nested
//    @DisplayName("isTransactionOwner")
//    class IsTransactionOwnerTest {
//
//        @Test
//        @DisplayName("should return true when user owns the transaction")
//        void isTransactionOwner_userOwnsTransaction_returnsTrue() {
//            CustomUserDetails userDetails = buildUserDetails(USER_ID);
//            when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(buildTransactionOwnedBy(USER_ID)));
//
//            assertThat(securityService.isTransactionOwner(TRANSACTION_ID, userDetails)).isTrue();
//        }
//
//        @Test
//        @DisplayName("should return false when user does not own the transaction")
//        void isTransactionOwner_differentUser_returnsFalse() {
//            CustomUserDetails userDetails = buildUserDetails(USER_ID);
//            when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(buildTransactionOwnedBy(999L)));
//
//            assertThat(securityService.isTransactionOwner(TRANSACTION_ID, userDetails)).isFalse();
//        }
//
//        @Test
//        @DisplayName("should return false when transaction is not found")
//        void isTransactionOwner_transactionNotFound_returnsFalse() {
//            CustomUserDetails userDetails = buildUserDetails(USER_ID);
//            when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());
//
//            assertThat(securityService.isTransactionOwner(TRANSACTION_ID, userDetails)).isFalse();
//        }
//
//        @Test
//        @DisplayName("should return false when transactionId is null")
//        void isTransactionOwner_nullTransactionId_returnsFalse() {
//            CustomUserDetails userDetails = buildUserDetails(USER_ID);
//
//            assertThat(securityService.isTransactionOwner(null, userDetails)).isFalse();
//        }
//
//        @Test
//        @DisplayName("should return false when userDetails is null")
//        void isTransactionOwner_nullUserDetails_returnsFalse() {
//            assertThat(securityService.isTransactionOwner(TRANSACTION_ID, null)).isFalse();
//        }
//    }
//}
