package edu.sjsu.wildstore.meta;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@TestConfiguration
public class TestOAuthSecurityConfig {
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return registrationId -> null;
    }
}
