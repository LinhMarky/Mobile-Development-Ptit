package com.homely.rental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
public class RentalApplication {

    public static void main(String[] args) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new SimpleCommandLinePropertySource(args));
        String activeProfiles = environment.getProperty("spring.profiles.active", "");
        boolean student14 =
                Arrays.stream(activeProfiles.split(","))
                        .map(String::trim)
                        .anyMatch("student14"::equals);
        SpringApplication.run(
                student14
                        ? com.homely.student14.Student14Application.class
                        : RentalApplication.class,
                args);
    }
}
