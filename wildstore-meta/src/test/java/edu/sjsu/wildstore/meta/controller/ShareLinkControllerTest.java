package edu.sjsu.wildstore.meta.controller;

import edu.sjsu.wildstore.Metadata;
import edu.sjsu.wildstore.ShareLink;
import edu.sjsu.wildstore.meta.MongoCollections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ShareLinkControllerTest extends BaseControllerTest {

    private static final String OWNER_EMAIL = "shareowner@example.com";
    private ShareLink testLink;

    private org.springframework.test.web.servlet.request.RequestPostProcessor ownerAuth() {
        return opaqueToken()
                .attributes(a -> { a.put("email", OWNER_EMAIL); a.put("name", "Owner"); })
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminAuth() {
        return opaqueToken()
                .attributes(a -> { a.put("email", "admin@example.com"); a.put("name", "Admin"); })
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor guestAuth() {
        return opaqueToken()
                .attributes(a -> { a.put("email", "guest@example.com"); a.put("name", "Guest"); })
                .authorities(new SimpleGrantedAuthority("ROLE_GUEST"));
    }

    @BeforeEach
    void setup() {
        mongoTemplate.getDb().getCollection(MongoCollections.SHARE_LINKS).drop();
        mongoTemplate.getDb().getCollection(MongoCollections.METADATA).drop();

        Metadata m = new Metadata();
        m.digestString = "shareDigest001";
        m.fileName = new HashSet<>(Set.of("/data/sharefile.nc"));
        m.filePath = new HashSet<>(Set.of("/data/"));
        m.lastModified = System.currentTimeMillis();
        mongoTemplate.save(m, MongoCollections.METADATA);

        testLink = new ShareLink();
        testLink.shareId = UUID.randomUUID().toString().replace("-", "");
        testLink.createdBy = OWNER_EMAIL;
        testLink.fileDigest = "shareDigest001";
        testLink.filePath = new HashSet<>(Set.of("/data/"));
        testLink.emailAddresses = new HashSet<>(Set.of("recipient@example.com"));
        testLink.createdAt = LocalDateTime.now();
        testLink.expiry = LocalDateTime.now().plusDays(7);
        mongoTemplate.save(testLink, MongoCollections.SHARE_LINKS);
    }

    @AfterEach
    void cleanup() {
        mongoTemplate.getDb().getCollection(MongoCollections.SHARE_LINKS).drop();
        mongoTemplate.getDb().getCollection(MongoCollections.METADATA).drop();
    }

    @Test
    void testListAndCount() throws Exception {
        // Unauthenticated, expect: 401
        mockMvc.perform(get("/api/share-link/"))
                .andExpect(status().isUnauthorized());

        // Owner sees their link
        mockMvc.perform(get("/api/share-link/").with(ownerAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Different user sees nothing
        mockMvc.perform(get("/api/share-link/")
                        .with(opaqueToken()
                                .attributes(a -> { a.put("email", "other@example.com"); a.put("name", "Other"); })
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Count
        mockMvc.perform(get("/api/share-link/count").with(ownerAuth()))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    void testCreate() throws Exception {
        // Unauthenticated, expect: 401
        mockMvc.perform(post("/api/share-link/create")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        // Known file, expect: share link created
        String body = "{\"fileNames\":[\"/data/sharefile.nc\"],"
                + "\"emailAddresses\":[\"r@example.com\"],\"validFor\":\"week\"}";
        mockMvc.perform(post("/api/share-link/create")
                        .with(ownerAuth())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.missing").exists());

        // Non-existent file, expect: empty created list
        String noFile = "{\"fileNames\":[\"/data/nonexistent.nc\"],"
                + "\"emailAddresses\":[\"r@example.com\"],\"validFor\":\"day\"}";
        mockMvc.perform(post("/api/share-link/create")
                        .with(ownerAuth())
                        .contentType(MediaType.APPLICATION_JSON).content(noFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created", hasSize(0)));
    }

    @Test
    void testDeleteShareLink() throws Exception {
        // Unauthenticated, expect: 401
        mockMvc.perform(delete("/api/share-link/" + testLink.shareId))
                .andExpect(status().isUnauthorized());

        // Non-owner cannot delete, expect: false
        mockMvc.perform(delete("/api/share-link/" + testLink.shareId)
                        .with(opaqueToken()
                                .attributes(a -> { a.put("email", "other@example.com"); a.put("name", "Other"); })
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        // Owner can delete, expect: true
        mockMvc.perform(delete("/api/share-link/" + testLink.shareId).with(ownerAuth()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testAdminDeleteShareLinks() throws Exception {
        // Non-admin, expect: forbidden
        mockMvc.perform(post("/api/share-link/delete")
                        .with(ownerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"" + testLink.shareId + "\"]"))
                .andExpect(status().isForbidden());

        // Admin, expect: success
        mockMvc.perform(post("/api/share-link/delete")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"" + testLink.shareId + "\"]"))
                .andExpect(status().isOk());
    }

    @Test
    void testVerify() throws Exception {
        // Valid share ID, expect: metadata returned
        mockMvc.perform(post("/api/share-link/verify")
                        .with(guestAuth())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(testLink.shareId))
                .andExpect(status().isOk());

        // Invalid share ID, expect: 404
        mockMvc.perform(post("/api/share-link/verify")
                        .with(guestAuth())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("nonExistentId"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAddDownloadHistory() throws Exception {
        // Unauthenticated, expect: 401
        mockMvc.perform(post("/api/share-link/downloadhistory")
                        .contentType(MediaType.APPLICATION_JSON).content("\"someId\""))
                .andExpect(status().isUnauthorized());

        // Authenticated guest with valid share ID, expect: 0 (success)
        mockMvc.perform(post("/api/share-link/downloadhistory")
                        .with(guestAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"" + testLink.shareId + "\""))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}