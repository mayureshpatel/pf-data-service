package com.mayureshpatel.pfdataservice.exception;

import com.mayureshpatel.pfdataservice.controller.AccountController;
import com.mayureshpatel.pfdataservice.security.JwtService;
import com.mayureshpatel.pfdataservice.security.WithCustomMockUser;
import com.mayureshpatel.pfdataservice.service.AccountService;
import com.mayureshpatel.pfdataservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@DisplayName("GlobalExceptionHandler tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private static final long USER_ID = 1L;

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("should return 404 with ProblemDetail for ResourceNotFoundException")
    void resourceNotFound_returns404WithProblemDetail() throws Exception {
        when(accountService.getAllAccountsByUserId(USER_ID))
                .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found"))
                .andExpect(jsonPath("$.instance").value("/api/v1/accounts"));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("should return 400 with ProblemDetail for IllegalArgumentException")
    void illegalArgument_returns400WithProblemDetail() throws Exception {
        when(accountService.getAllAccountsByUserId(USER_ID))
                .thenThrow(new IllegalArgumentException("Invalid account ID"));

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid account ID"));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("should return 400 with ProblemDetail for DataIntegrityViolationException")
    void dataIntegrityViolation_returns400WithProblemDetail() throws Exception {
        doThrow(new DataIntegrityViolationException("FK constraint"))
                .when(accountService).deleteAccount(eq(USER_ID), eq(1L));

        mockMvc.perform(delete("/api/v1/accounts/{id}", 1L)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Database constraint violation. Please check your input data."));
    }

    @Test
    @WithCustomMockUser(id = USER_ID)
    @DisplayName("should return 500 with ProblemDetail for unexpected exceptions")
    void unexpectedException_returns500WithProblemDetail() throws Exception {
        when(accountService.getAllAccountsByUserId(USER_ID))
                .thenThrow(new RuntimeException("Something went wrong"));

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected internal error occurred. Please contact support."));
    }
}
