package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.repository.transaction.JdbcCurrencyRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.model.Currency;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrencyService {

    private final JdbcCurrencyRepository currencyRepository;

    public List<Currency> getAllActiveCurrencies() {
        return this.currencyRepository.findByIsActive(true);
    }

    public Currency getCurrencyByCode(String code) throws EntityNotFoundException {
        return this.currencyRepository.findById(code)
                .orElseThrow(() -> new EntityNotFoundException("Currency with code " + code + " not found"));
    }
}
