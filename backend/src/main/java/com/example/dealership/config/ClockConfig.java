package com.example.dealership.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Warsaw"));
    }

    @Bean
    public ValidationConfigurationCustomizer clockProviderCustomizer(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }
}
