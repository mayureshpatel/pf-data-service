package com.mayureshpatel.pfdataservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Smoke-tests that the full Spring context loads cleanly, plus a PF-813 regression guard: two independent {@code RequestLoggingFilter} classes (one under {@code filter}, one a stray top-level duplicate) once coexisted, so this asserts exactly one {@code RequestLoggingFilter}-named bean is ever registered, and that it resolves to the real, intended class. */
class PfDataServiceApplicationTests extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // intentionally empty: passes as long as the spring context loads without error
    }

    @Test
    void shouldRegisterExactlyOneRequestLoggingFilterBean() {
        // arrange & act -- PF-813: two independent RequestLoggingFilter classes once coexisted
        // (com.mayureshpatel.pfdataservice.filter.RequestLoggingFilter and a stray top-level
        // filter.RequestLoggingFilter); a plain bean-count check here is the most direct guard
        // against this exact duplication recurring
        String[] beanNames = applicationContext.getBeanNamesForType(OncePerRequestFilter.class);
        List<String> requestLoggingFilterBeans = Arrays.stream(beanNames)
                .filter(name -> applicationContext.getType(name) != null
                        && "RequestLoggingFilter".equals(applicationContext.getType(name).getSimpleName()))
                .toList();

        // assert & verify
        assertThat(requestLoggingFilterBeans).hasSize(1);
        Class<?> registeredType = applicationContext.getType(requestLoggingFilterBeans.get(0));
        assertThat(registeredType).isNotNull();
        assertThat(registeredType.getName())
                .isEqualTo("com.mayureshpatel.pfdataservice.filter.RequestLoggingFilter");
    }

}
