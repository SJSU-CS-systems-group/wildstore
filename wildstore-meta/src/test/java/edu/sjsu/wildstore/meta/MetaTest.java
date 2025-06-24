package edu.sjsu.wildstore.meta;

import edu.sjsu.wildstore.meta.service.UserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.ServerSocket;

@SpringBootTest(classes = {TestOAuthSecurityConfig.class, Main.class})
@TestPropertySource(locations = {"classpath:application-test.properties"})
public class MetaTest {

    @Autowired
    UserService userService;

    @Autowired
    MongoTemplate mongoTemplate;

    static MongoTemplate staticMongoTemplate;

    @DynamicPropertySource
    static void setProps(DynamicPropertyRegistry registry) throws IOException {
        var sock = new ServerSocket(0);
        var port = sock.getLocalPort();
        sock.close();

        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/wildstore-test-" + System.currentTimeMillis());
        registry.add("server.port", () -> port);
        registry.add("wildstore.initialAdmins", () -> "test@example.com");
    }

    @BeforeEach
    public void setup() {
        if (staticMongoTemplate == null) {
            staticMongoTemplate = mongoTemplate;
        }
    }

    @AfterAll
    public static void teardown() {
        if (staticMongoTemplate != null) {
            staticMongoTemplate.getDb().drop();
        }
    }

    @Test
    public void testInitialAdmin() {
        var userList = userService.getUserList();
        Assertions.assertEquals(1, userList.size());
        Assertions.assertEquals("ROLE_ADMIN", userList.get(0).get("role"));
    }
}
