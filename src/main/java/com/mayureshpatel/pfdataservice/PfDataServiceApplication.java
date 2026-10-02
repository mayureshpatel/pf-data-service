package com.mayureshpatel.pfdataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/** Application entry point. */
@SuppressWarnings("PMD.UseUtilityClass")
@SpringBootApplication
public class PfDataServiceApplication {

    /**
     * Forces the JVM default timezone to UTC before Spring starts -- every timestamp this
     * application stores or compares is meant to be UTC-anchored (see
     * {@code TransactionRepository}'s own {@code UTC_ZONE} field comment for a concrete example
     * of why), and setting this once here means individual call sites don't each need to opt into
     * UTC explicitly to avoid the JVM's own local timezone leaking in.
     *
     * @param args command-line arguments, passed through to Spring Boot unchanged
     */
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(PfDataServiceApplication.class, args);
    }

}
