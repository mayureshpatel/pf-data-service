package com.mayureshpatel.pfdataservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Wiring for session renewal (PF-899): binds {@link RefreshTokenProperties}, exposes the UTC
 * {@link Clock} that time-dependent services inject (so tests can fix time), and turns on
 * scheduling for the daily refresh-token purge (the app had no scheduling before this).
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(RefreshTokenProperties.class)
public class SessionConfig {

    /**
     * @return the system UTC clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
