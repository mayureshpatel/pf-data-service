package com.mayureshpatel.pfdataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayureshpatel.pfdataservice.config.SecurityConfig;
import com.mayureshpatel.pfdataservice.repository.account.AccountTypeRepository;
import com.mayureshpatel.pfdataservice.security.JwtAuthenticationFilter;
import com.mayureshpatel.pfdataservice.security.JwtService;
import com.mayureshpatel.pfdataservice.security.SecurityService;
import com.mayureshpatel.pfdataservice.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for all Controller tests to ensure ApplicationContext reuse.
 * By defining all shared mocks here, Spring caches the context for the entire suite.
 */
@WebMvcTest
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // --- Shared Mocks ---
    // Centralizing all mocks here ensures the ApplicationContext is identical across all controller tests.
    // This allows Spring to reuse the context and avoids expensive context reloads.

    @MockitoBean
    protected AccountService accountService;
    @MockitoBean
    protected AccountTypeRepository accountTypeRepository;
    @MockitoBean
    protected AuthenticationService authenticationService;
    @MockitoBean
    protected RegistrationService registrationService;
    @MockitoBean
    protected BudgetService budgetService;
    @MockitoBean
    protected CategoryService categoryService;
    @MockitoBean
    protected CategoryRuleService categoryRuleService;
    @MockitoBean
    protected DashboardService dashboardService;
    @MockitoBean
    protected RecurringTransactionService recurringTransactionService;
    @MockitoBean
    protected TransactionService transactionService;
    @MockitoBean
    protected TransactionImportService transactionImportService;
    @MockitoBean
    protected UserService userService;
    @MockitoBean
    protected JwtService jwtService;
    @MockitoBean(name = "ss")
    protected SecurityService securityService;
    @MockitoBean
    protected SnapshotService snapshotService;
    @MockitoBean
    protected CurrencyService currencyService;

    // User ID constant for convenience
    protected static final long USER_ID = 1L;
}
