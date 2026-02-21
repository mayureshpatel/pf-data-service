package com.mayureshpatel.pfdataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class PfDataServiceApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        SpringApplication.run(PfDataServiceApplication.class, args);
    }

}
