package com.sjsu.wildfirestorage.spring.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(ac -> ac.requestMatchers("/api/**").permitAll().anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults()).logout(Customizer.withDefaults());
        return http.build();
    }
}
