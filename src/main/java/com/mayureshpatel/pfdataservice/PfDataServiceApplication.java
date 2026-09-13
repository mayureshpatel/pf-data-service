package com.mayureshpatel.pfdataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SuppressWarnings("PMD.UseUtilityClass")
@SpringBootApplication
public class PfDataServiceApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(PfDataServiceApplication.class, args);
    }

}
