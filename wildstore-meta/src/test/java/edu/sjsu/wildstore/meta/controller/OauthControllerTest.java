package edu.sjsu.wildstore.meta.controller;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OauthControllerTest extends BaseControllerTest {

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
    }
}