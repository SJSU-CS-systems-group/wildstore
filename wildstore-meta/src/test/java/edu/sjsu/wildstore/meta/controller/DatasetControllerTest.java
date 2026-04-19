package edu.sjsu.wildstore.meta.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DatasetControllerTest extends BaseControllerTest {

    @AfterEach
    void cleanup() {
        mongoTemplate.getDb().getCollection("dataset").drop();
        mongoTemplate.getDb().getCollection("metadata").drop();
    }

    @Test
    void testEncodingBase64() {
        // Empty string decodes to BigInteger.ZERO
        assertEquals(BigInteger.ZERO, DatasetController.base64decoding(""));

        // Valid base64 string decodes to a non-zero BigInteger
        byte[] bytes = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        String encoded = Base64.getUrlEncoder().encodeToString(bytes);
        BigInteger decoded = DatasetController.base64decoding(encoded);
        assertNotNull(decoded);
        assertNotEquals(BigInteger.ZERO, decoded);

        // Encoding produces exactly 16 characters (12 bytes, exactly 16 base64 chars)
        BigInteger value = new BigInteger(1, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        String result = DatasetController.base64encoding(value);
        assertEquals(16, result.length());

        // Two different large values → two different encodings (values must encode to ≥16 chars)
        BigInteger a = new BigInteger(1, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        BigInteger b = new BigInteger(1, new byte[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13});
        assertNotEquals(DatasetController.base64encoding(a), DatasetController.base64encoding(b));
    }

    @Test
    void testUpsertDataset() throws Exception {
        // Unauthenticated, expect: 401
        mockMvc.perform(post("/api/dataset"))
                .andExpect(status().isUnauthorized());

        // Authenticated USER, expect: 200 OK (returns 0 on empty DB)
        mockMvc.perform(post("/api/dataset").with(user("testuser").roles("USER")))
                .andExpect(status().isOk());
    }
}