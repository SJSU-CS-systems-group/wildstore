package edu.sjsu.wildstore.meta.controller;

import edu.sjsu.wildstore.meta.MongoCollections;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest extends BaseControllerTest {

    @BeforeEach
    void ensureAdminUserExists() {
        Query q = new Query(Criteria.where("email").is("admin@example.com"));
        if (mongoTemplate.find(q, Map.class, MongoCollections.USER_DATA).isEmpty()) {
            Map<String, Object> u = new HashMap<>();
            u.put("email", "admin@example.com");
            u.put("name", "admin");
            u.put("role", "ROLE_ADMIN");
            u.put("token", OauthController.generateToken());
            mongoTemplate.insert(u, MongoCollections.USER_DATA);
        }
    }

    private static ClientRegistration googleRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("test-client")
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();
    }

    @Test
    void testGetUserProfile() throws Exception {
        // Unauthenticated, expect: 401
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());

        // Google login with existing admin user, expect: 200 with user data
        mockMvc.perform(get("/api/user/me")
                        .with(oauth2Login()
                                .clientRegistration(googleRegistration())
                                .attributes(attrs -> {
                                    attrs.put("email", "admin@example.com");
                                    attrs.put("name", "Admin User");
                                })
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));

        // Unsupported provider (registrationId "test"), expect: throws IllegalArgumentException, ServletException
        assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/api/user/me")
                        .with(oauth2Login()
                                .attributes(attrs -> attrs.put("email", "admin@example.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))));

        // Known provider but user not in DB, expect: throws IllegalArgumentException, ServletException
        assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/api/user/me")
                        .with(oauth2Login()
                                .clientRegistration(googleRegistration())
                                .attributes(attrs -> {
                                    attrs.put("email", "nobody@example.com");
                                    attrs.put("name", "Nobody");
                                })
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))));
    }
}