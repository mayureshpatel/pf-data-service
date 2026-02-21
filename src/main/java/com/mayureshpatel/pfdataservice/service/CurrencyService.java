package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    /**
     * Gets all active currencies from the repository.
     *
     * @return list of {@link Currency} objects
     */
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
