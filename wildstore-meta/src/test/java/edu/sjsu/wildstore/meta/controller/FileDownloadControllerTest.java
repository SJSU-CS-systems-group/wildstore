package edu.sjsu.wildstore.meta.controller;

import org.junit.jupiter.api.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FileDownloadControllerTest extends BaseControllerTest {

    @Test
    void testDownloadFile() throws Exception {
        // Unauthenticated, expect: 401
        mockMvc.perform(get("/api/file/someDigest"))
                .andExpect(status().isUnauthorized());

        // GUEST, expect: forbidden
        mockMvc.perform(get("/api/file/someDigest").with(user("g").roles("GUEST")))
                .andExpect(status().isForbidden());

        // USER, expect: redirects to file server with the digest in the URL
        mockMvc.perform(get("/api/file/abc123").with(user("u").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:27778/api/file/abc123"));

        // URL-safe digest characters are preserved in redirect
        mockMvc.perform(get("/api/file/XyZ-abc_456").with(user("u").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:27778/api/file/XyZ-abc_456"));
    }
}