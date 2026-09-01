package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only endpoint for the set of currencies the application supports.
 */
@Tag(name = "Currencies", description = "Supported currencies")
@RestController
@RequestMapping("/api/v1/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    /**
     * Returns every currently active currency.
     *
     * @return the active currencies
     */
    @Operation(summary = "List active currencies", description = "Returns every currently active currency")
    @ApiResponse(responseCode = "200", description = "Currencies returned")
    @GetMapping
    public ResponseEntity<List<Currency>> getCurrencies() {
        List<Currency> currencies = currencyService.getAllActiveCurrencies();
        return ResponseEntity.ok(currencies);
    }
}
