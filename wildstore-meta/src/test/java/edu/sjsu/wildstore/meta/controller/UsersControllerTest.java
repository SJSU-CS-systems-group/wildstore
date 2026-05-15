package edu.sjsu.wildstore.meta.controller;

import edu.sjsu.wildstore.meta.MongoCollections;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UsersControllerTest extends BaseControllerTest {

    @BeforeEach
    void setup() {
        Query q = new Query(Criteria.where("email").is("admin@example.com"));
        if (mongoTemplate.find(q, Map.class, MongoCollections.USER_DATA).isEmpty()) {
            Map<String, Object> admin = new HashMap<>();
            admin.put("email", "admin@example.com");
            admin.put("name", "admin");
            admin.put("role", "ROLE_ADMIN");
            admin.put("token", OauthController.generateToken());
            mongoTemplate.insert(admin, MongoCollections.USER_DATA);
        }

        mongoTemplate.remove(new Query(Criteria.where("email").is("user@example.com")), MongoCollections.USER_DATA);
        Map<String, Object> testUser = new HashMap<>();
        testUser.put("email", "user@example.com");
        testUser.put("name", "testuser");
        testUser.put("role", "ROLE_USER");
        testUser.put("token", OauthController.generateToken());
        mongoTemplate.insert(testUser, MongoCollections.USER_DATA);
    }

    @AfterEach
    void cleanup() {
        mongoTemplate.remove(new Query(Criteria.where("email").is("user@example.com")), MongoCollections.USER_DATA);
        mongoTemplate.remove(new Query(Criteria.where("email").is("newuser@example.com")), MongoCollections.USER_DATA);
    }

    @Test
    void testGetUserList() throws Exception {
        // Unauthenticated, expect: 401
        mockMvc.perform(get("/api/userlist/"))
                .andExpect(status().isUnauthorized());

        // Non-admin, expect: forbidden
        mockMvc.perform(get("/api/userlist/").with(user("u").roles("USER")))
                .andExpect(status().isForbidden());

        // Admin, expect: 200 with at least the two seeded users
        mockMvc.perform(get("/api/userlist/").with(user("a").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void testGetUserByEmail() throws Exception {
        // Known email, expect: list with one entry
        mockMvc.perform(get("/api/userlist/admin@example.com").with(user("a").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Unknown email, expect: empty list
        mockMvc.perform(get("/api/userlist/nobody@example.com").with(user("a").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testUpdateUserRole() throws Exception {
        // Valid role update, expect: 200
        mockMvc.perform(post("/api/userlist/user@example.com")
                        .with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isOk());

        // Create-on-update for new user, expect: 200
        mockMvc.perform(post("/api/userlist/newuser@example.com")
                        .with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_GUEST\"}"))
                .andExpect(status().isOk());

        // Invalid role, expect: controller throws IllegalArgumentException, and ServletException
        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/api/userlist/user@example.com")
                        .with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_INVALID\"}")));

        // Missing role field, expect: controller throws IllegalArgumentException, and ServletException
        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/api/userlist/user@example.com")
                        .with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")));
    }

    @Test
    void testDeleteUser() throws Exception {
        // Delete existing user, expect: true
        mockMvc.perform(delete("/api/userlist/user@example.com").with(user("a").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // Delete non-existent user, expect: false
        mockMvc.perform(delete("/api/userlist/nobody@example.com").with(user("a").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}