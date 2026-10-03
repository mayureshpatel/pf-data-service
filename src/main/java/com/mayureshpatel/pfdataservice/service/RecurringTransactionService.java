package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringSuggestionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionDto;
import com.mayureshpatel.pfdataservice.dto.transaction.recurring.RecurringTransactionUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.RecurringTransactionDtoMapper;
import com.mayureshpatel.pfdataservice.repository.account.AccountRepository;
import com.mayureshpatel.pfdataservice.repository.merchant.MerchantRepository;
import com.mayureshpatel.pfdataservice.repository.recurring_history.RecurringTransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD for a user's confirmed recurring transactions. Pattern detection (finding new candidates
 * from raw transaction history) is {@link RecurringSuggestionFinder}'s own concern, not this
 * class's -- the two used to be one class until splitting detection's own internals into enough
 * named helper methods to resolve its complexity findings (PF-809) left this class newly flagged
 * as a PMD {@code GodClass}, since most of those new helpers are pure functions that don't touch
 * either class's instance state and lowered Tight Class Cohesion as a result.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final RecurringSuggestionFinder suggestionFinder;

    /**
     * Returns the user's active confirmed recurring transactions, next-occurrence first.
     *
     * @param userId the user id
     * @return the user's recurring transactions
     */
    public List<RecurringTransactionDto> getRecurringTransactions(Long userId) {
        return recurringRepository.findByUserIdAndActiveTrueOrderByNextDate(userId).stream()
                .map(RecurringTransactionDtoMapper::toDto)
                .toList();
    }

    /**
     * Detects candidate recurring transaction patterns in the last 12 months of the user's
     * expenses that aren't already confirmed as recurring, ranked by confidence (higher
     * occurrence counts score higher). Delegates entirely to {@link RecurringSuggestionFinder}.
     *
     * @param userId the user id
     * @return the detected suggestions, highest confidence first
     */
    public List<RecurringSuggestionDto> findSuggestions(Long userId) {
        return suggestionFinder.findSuggestions(userId);
    }

    /**
     * Confirms a new recurring transaction, verifying the account (if given) and merchant (if
     * given) both exist and, for the account, belong to the user.
     *
     * @param userId  the user id
     * @param request the recurring transaction to create
     * @return the new record's generated id
     */
    @Transactional
    public int createRecurringTransaction(Long userId, RecurringTransactionCreateRequest request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getAccountId() != null) {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            if (!account.getUserId().equals(userId)) {
                throw new AccessDeniedException("Access denied to account");
            }
        }

        if (request.getMerchantId() != null && request.getMerchantId() > 0) {
            merchantRepository.findById(request.getMerchantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
        }

        return recurringRepository.insert(request, userId);
    }

    /**
     * Updates an existing recurring transaction owned by the user, re-verifying the account (if
     * given) and merchant (if given) the same way {@link #createRecurringTransaction} does.
     *
     * @param userId  the user id
     * @param request the recurring transaction to update, including its id
     * @return the number of rows updated
     * @throws ResourceNotFoundException if no recurring transaction with that id exists
     * @throws AccessDeniedException     if it belongs to a different user
     */
    @Transactional
    public int updateRecurringTransaction(Long userId, RecurringTransactionUpdateRequest request) {
        RecurringTransaction recurring = recurringRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found"));

        if (!recurring.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        if (request.getAccountId() != null) {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

            if (!account.getUserId().equals(userId)) {
                throw new AccessDeniedException("Access denied to account");
            }
        }

        if (request.getMerchantId() != null && request.getMerchantId() > 0) {
            merchantRepository.findById(request.getMerchantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
        }

        return recurringRepository.update(request, userId);
    }

    /**
     * Deletes a recurring transaction owned by the user.
     *
     * @param userId the user id
     * @param id     the recurring transaction id to delete
     * @return the number of rows deleted
     * @throws ResourceNotFoundException if no recurring transaction with that id exists
     * @throws AccessDeniedException     if it belongs to a different user
     */
    @Transactional
    public int deleteRecurringTransaction(Long userId, Long id) {
        RecurringTransaction recurring = recurringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found"));

        if (!recurring.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        return recurringRepository.delete(id, userId);
    }
}
