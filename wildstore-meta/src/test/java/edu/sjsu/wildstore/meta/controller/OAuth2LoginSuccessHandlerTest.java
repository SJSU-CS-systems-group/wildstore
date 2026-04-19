package edu.sjsu.wildstore.meta.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OAuth2LoginSuccessHandlerTest {

    @Test
    void testOnAuthenticationSuccess() throws Exception {
        OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler();
        var auth = new UsernamePasswordAuthenticationToken(
                "user", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // Default frontendUrl is "/" per @Value default
        ReflectionTestUtils.setField(handler, "frontendUrl", "/");
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(req, res, auth);
        assertEquals(302, res.getStatus());
        assertEquals("/", res.getRedirectedUrl());

        // Custom frontendUrl
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:3000");
        req = new MockHttpServletRequest();
        res = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(req, res, auth);
        assertEquals(302, res.getStatus());
        assertEquals("http://localhost:3000", res.getRedirectedUrl());
    }
}