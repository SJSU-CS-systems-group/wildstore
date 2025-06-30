package edu.sjsu.wildstore.meta;

import edu.sjsu.wildstore.meta.service.UserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

@SpringBootTest(classes = {TestOAuthSecurityConfig.class, Main.class})
@TestPropertySource(locations = {"classpath:application-test.properties"})
@EnableScheduling
public class MetaTest {

    @Autowired
    UserService userService;

    @Autowired
    MongoTemplate mongoTemplate;

    static MongoTemplate staticMongoTemplate;

    @TempDir
    static Path backupDir;

    @DynamicPropertySource
    static void setProps(DynamicPropertyRegistry registry) throws IOException {
        var sock = new ServerSocket(0);
        var port = sock.getLocalPort();
        sock.close();

        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/wildstore-test-" + System.currentTimeMillis());
        registry.add("server.port", () -> port);
        registry.add("wildstore.initialAdmins", () -> "test@example.com");
        registry.add("wildstore.backupDirectory", () -> backupDir.toString());
        registry.add("backup.interval", () -> "500"); // .5 second for testing
    }

    @BeforeEach
    public void setup() {
        if (staticMongoTemplate == null) {
            staticMongoTemplate = mongoTemplate;
        }
    }

    @AfterAll
    public static void teardown() throws IOException {
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

    @Test
    public void testBackup() throws IOException, InterruptedException {
        FileTime firstTime = Files.getLastModifiedTime(backupDir.resolve("share-links.json"));
        Assertions.assertTrue(Files.exists(backupDir.resolve("share-links.json")));
        Assertions.assertTrue(Files.exists(backupDir.resolve("userData.json")));

        Thread.sleep(1000);
        Assertions.assertTrue(firstTime.compareTo(Files.getLastModifiedTime(backupDir.resolve("share-links.json"))) < 0);

    }
}
