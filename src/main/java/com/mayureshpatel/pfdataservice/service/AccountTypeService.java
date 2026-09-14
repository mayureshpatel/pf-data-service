package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.config.CacheConfig;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;
import com.mayureshpatel.pfdataservice.mapper.AccountTypeDtoMapper;
import com.mayureshpatel.pfdataservice.repository.account.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read/write access to the shared account-type reference data (checking, savings, credit card,
 * etc.). Introduced by PF-322 -- {@link com.mayureshpatel.pfdataservice.controller.AccountTypeController}
 * previously called {@link AccountTypeRepository} directly, skipping this project's own
 * mandated Controller-Service-Repository layering; caching this resource's real
 * create/delete write paths via {@code @CacheEvict} needs a Service-layer method to attach to,
 * matching the {@link CurrencyService} precedent this same epic already established.
 */
@Service
@RequiredArgsConstructor
public class AccountTypeService {

    private final AccountTypeRepository accountTypeRepository;

    /**
     * Gets every active account type, in display order. Cached (PF-322): see
     * {@link CacheConfig#ACCOUNT_TYPES_CACHE} for the TTL/eviction rationale.
     *
     * @return the active account types
     */
    @Cacheable(CacheConfig.ACCOUNT_TYPES_CACHE)
    public List<AccountTypeDto> getAllActiveAccountTypes() {
        return AccountTypeDtoMapper.toDto(accountTypeRepository.findByIsActiveTrueOrderBySortOrder());
    }

    /**
     * Creates a new account type, evicting the cached list so the new type is visible on the
     * very next read on this instance.
     *
     * @param request the account type to create
     * @return the number of rows inserted
     */
    @CacheEvict(value = CacheConfig.ACCOUNT_TYPES_CACHE, allEntries = true)
    public int create(AccountTypeCreateRequest request) {
        return accountTypeRepository.insert(request);
    }

    /**
     * Deletes an account type by its code, evicting the cached list so the removal is visible on
     * the very next read on this instance.
     *
     * @param code the account type code to delete
     * @return the number of rows deleted
     */
    @CacheEvict(value = CacheConfig.ACCOUNT_TYPES_CACHE, allEntries = true)
    public int delete(String code) {
        return accountTypeRepository.deleteByCode(code);
    }
}
