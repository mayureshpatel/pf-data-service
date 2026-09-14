package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.config.CacheConfig;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read access to the shared currency reference data. {@link CurrencyController} only exposes
 * these read operations; the repository layer has write methods too, but no controller currently
 * calls them.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    /**
     * Gets all active currencies from the repository. Cached (PF-321): this list is genuinely
     * static via the API (no create/update/delete endpoint exists), so repeated calls within the
     * 24-hour TTL configured in {@link CacheConfig} are served from memory instead of hitting the
     * database.
     *
     * @return list of {@link Currency} objects
     */
    @Cacheable(CacheConfig.CURRENCIES_CACHE)
    public List<Currency> getAllActiveCurrencies() {
        return this.currencyRepository.findByIsActive();
    }

    /**
     * Gets a currency by its code.
     *
     * @param code the currency code
     * @return {@link Currency} object
     * @throws ResourceNotFoundException if currency with given code is not found
     */
    public Currency getCurrencyByCode(String code) throws ResourceNotFoundException {
        return this.currencyRepository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Currency with code " + code + " not found"));
    }
}
