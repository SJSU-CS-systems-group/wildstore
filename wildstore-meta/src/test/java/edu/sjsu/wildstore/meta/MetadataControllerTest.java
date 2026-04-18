package edu.sjsu.wildstore.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sjsu.wildstore.Metadata;
import edu.sjsu.wildstore.meta.controller.OauthController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestOAuthSecurityConfig.class, Main.class})
@AutoConfigureMockMvc
@TestPropertySource(locations = {"classpath:application-test.properties"})
public class MetadataControllerTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    static MongoTemplate staticMongoTemplate;
    static String bearerToken;

    @DynamicPropertySource
    static void setProps(DynamicPropertyRegistry registry) throws IOException {
        var sock = new ServerSocket(0);
        var port = sock.getLocalPort();
        sock.close();

        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/wildstore-test-metacontroller-" + System.currentTimeMillis());
        registry.add("server.port", () -> port);
        registry.add("wildstore.initialAdmins", () -> "");
        registry.add("wildstore.backupDirectory", () -> {
            try {
                return Files.createTempDirectory("backup-metacontroller").toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @BeforeEach
    public void setup() {
        if (staticMongoTemplate == null) {
            staticMongoTemplate = mongoTemplate;
            bearerToken = OauthController.generateToken();
            Map<String, Object> user = new HashMap<>();
            user.put("name", "Test User");
            user.put("email", "test@example.com");
            user.put("token", bearerToken);
            user.put("role", "ROLE_ADMIN");
            mongoTemplate.insert(user, "userData");
        }
    }

    @AfterAll
    public static void teardown() {
        if (staticMongoTemplate != null) {
            staticMongoTemplate.getDb().drop();
        }
    }

    /**
     * Regression test: searching for a filename that contains regex special characters
     * (e.g. parentheses) must not cause a server error. Previously, fileName was
     * interpolated directly into the regex pattern without escaping.
     */
    @Test
    public void testSearchWithRegexSpecialCharsReturnsOk() throws Exception {
        mockMvc.perform(get("/api/metadata")
                        .param("filename", "file(1).nc")
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isOk());
    }

    /**
     * Regression test: upserting a Metadata object whose fileName set is empty
     * must not throw NoSuchElementException. Previously, iterator().next() was called
     * unconditionally on the fileName set.
     */
    @Test
    public void testUpsertWithEmptyFileNameReturnsOk() throws Exception {
        Metadata metadata = new Metadata();
        metadata.fileName = new HashSet<>();
        metadata.filePath = new HashSet<>();
        metadata.digestString = "deadbeef";
        metadata.globalAttributes = new ArrayList<>();
        metadata.variables = new ArrayList<>();
        metadata.lastModified = System.currentTimeMillis();

        mockMvc.perform(post("/api/metadata")
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(metadata)))
                .andExpect(status().isOk());
    }

    /**
     * Regression test: upserting a Metadata object with a null fileName field
     * (i.e. the field is absent from the JSON body) must not throw NullPointerException.
     */
    @Test
    public void testUpsertWithNullFileNameReturnsOk() throws Exception {
        Metadata metadata = new Metadata();
        // fileName intentionally left null
        metadata.filePath = new HashSet<>();
        metadata.digestString = "cafebabe";
        metadata.globalAttributes = new ArrayList<>();
        metadata.variables = new ArrayList<>();
        metadata.lastModified = System.currentTimeMillis();

        mockMvc.perform(post("/api/metadata")
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(metadata)))
                .andExpect(status().isOk());
    }
}
