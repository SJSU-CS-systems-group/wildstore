package edu.sjsu.wildstore.meta.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StatisticsControllerTest extends BaseControllerTest {

    @AfterEach
    void cleanup() {
        mongoTemplate.getDb().getCollection("metadata").drop();
        mongoTemplate.getDb().getCollection("dataset").drop();
    }

    @Test
    void testMetadata() throws Exception {
        // Unauthenticated, expect: 401
        mockMvc.perform(get("/api/stats/metadataBasic").param("collectionName", "metadata"))
                .andExpect(status().isUnauthorized());

        // GUEST is below USER in hierarchy, expect: forbidden
        mockMvc.perform(get("/api/stats/metadataBasic")
                        .param("collectionName", "metadata")
                        .with(user("guest").roles("GUEST")))
                .andExpect(status().isForbidden());

        // USER on metadata collection, expect: 200 with all expected fields and zero counts on empty DB
        mockMvc.perform(get("/api/stats/metadataBasic")
                        .param("collectionName", "metadata")
                        .with(user("u").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectionSize").value("0"))
                .andExpect(jsonPath("$.duplicateSize").value("0"))
                .andExpect(jsonPath("$.numberOfVariables").exists())
                .andExpect(jsonPath("$.numberOfAttributes").exists());

        // USER on dataset collection, expect: 200 with expected fields
        mockMvc.perform(get("/api/stats/metadataBasic")
                        .param("collectionName", "dataset")
                        .with(user("u").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectionSize").value("0"))
                .andExpect(jsonPath("$.duplicateSize").value("0"));

        // Unknown collection name, expect: collectionSize 0
        mockMvc.perform(get("/api/stats/metadataBasic")
                        .param("collectionName", "unknown")
                        .with(user("u").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectionSize").value("0"));
    }
}