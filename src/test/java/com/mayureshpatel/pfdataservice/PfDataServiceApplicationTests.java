package com.mayureshpatel.pfdataservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
                        && applicationContext.getType(name).getSimpleName().equals("RequestLoggingFilter"))
                .toList();

        // assert & verify
        assertThat(requestLoggingFilterBeans).hasSize(1);
        Class<?> registeredType = applicationContext.getType(requestLoggingFilterBeans.get(0));
        assertThat(registeredType).isNotNull();
        assertThat(registeredType.getName())
                .isEqualTo("com.mayureshpatel.pfdataservice.filter.RequestLoggingFilter");
    }

}
