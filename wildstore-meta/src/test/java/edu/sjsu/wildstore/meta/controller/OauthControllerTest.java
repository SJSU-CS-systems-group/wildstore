package edu.sjsu.wildstore.meta.controller;

import edu.sjsu.wildstore.meta.MongoCollections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OauthControllerTest extends BaseControllerTest {

    @Autowired
    private OauthController oauthController;

    @AfterEach
    void cleanup() {
        mongoTemplate.remove(new Query(Criteria.where("email").is("oauthtest@example.com")), MongoCollections.USER_DATA);
    }

    @Test
    void testGenerateToken() {
        String t1 = OauthController.generateToken();
        String t2 = OauthController.generateToken();

        // Must be non-null, non-empty, and valid base64url-encoded
        assertNotNull(t1);
        assertTrue(t1.length() > 0);
        assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(t1));

        // Each call produces a unique token
        assertNotEquals(t1, t2);
    }

    @Test
    void testOauth() throws Exception {
        // /api/oauth/checkAccess:
        // unauthenticated, expect: 401,
        // GUEST, expect: forbidden,
        // USER, expect: ok
        mockMvc.perform(get("/api/oauth/checkAccess"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/oauth/checkAccess").with(user("g").roles("GUEST")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/oauth/checkAccess").with(user("u").roles("USER")))
                .andExpect(status().isOk());

        // /api/oauth/userInfo:
        // unauthenticated, expect: 401,
        // any user, expect: ok
        mockMvc.perform(get("/api/oauth/userInfo"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/oauth/userInfo").with(user("u").roles("USER")))
                .andExpect(status().isOk());

        // /api/oauth/token:
        // unauthenticated, expect: 401
        mockMvc.perform(get("/api/oauth/token"))
                .andExpect(status().isUnauthorized());

        // /api/oauth/token/regenerate:
        // unauthenticated, expect: 401
        mockMvc.perform(get("/api/oauth/token/regenerate"))
                .andExpect(status().isUnauthorized());

        // /api/oauth/user: Spring MVC cannot resolve OAuth2User without @AuthenticationPrincipal,
        // so set up SecurityContext and invoke the controller method directly
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
                "testuser", null, List.of(new SimpleGrantedAuthority("ROLE_GUEST"))));
        SecurityContextHolder.setContext(ctx);
        try {
            Map<String, Object> userAttrs = new HashMap<>();
            userAttrs.put("login", "testlogin");
            DefaultOAuth2User mockUser = new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority("ROLE_GUEST")), userAttrs, "login");
            ResponseEntity<String> userResponse = oauthController.user(mockUser);
            assertEquals(HttpStatus.OK, userResponse.getStatusCode());
            assertEquals("testlogin", userResponse.getBody());
        } finally {
            SecurityContextHolder.clearContext();
        }

        // /api/oauth/token: oauth2Login with email+name, expect: 200 (exercises getOpaqueToken())
        mockMvc.perform(get("/api/oauth/token")
                        .with(oauth2Login().attributes(attrs -> {
                            attrs.put("email", "oauthtest@example.com");
                            attrs.put("name", "OAuth Test");
                        }).authorities(new SimpleGrantedAuthority("ROLE_GUEST"))))
                .andExpect(status().isOk());

        // /api/oauth/token/regenerate: oauth2Login with email, expect: 200
        mockMvc.perform(get("/api/oauth/token/regenerate")
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "oauthtest@example.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_GUEST"))))
                .andExpect(status().isOk());
    }
}