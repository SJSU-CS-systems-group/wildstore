package edu.sjsu.wildstore.meta.controller;

import edu.sjsu.wildstore.meta.Main;
import edu.sjsu.wildstore.meta.TestOAuthSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.ServerSocket;

@SpringBootTest(classes = {TestOAuthSecurityConfig.class, Main.class})
@AutoConfigureMockMvc
@TestPropertySource(locations = {"classpath:application-test.properties"})
public abstract class BaseControllerTest {

    private static final int SERVER_PORT;

    static {
        try {
            ServerSocket sock = new ServerSocket(0);
            SERVER_PORT = sock.getLocalPort();
            sock.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void setProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/wildstore-ctrl-test");
        registry.add("server.port", () -> SERVER_PORT);
        registry.add("custom.fileServer", () -> "http://localhost:27778");
        registry.add("custom.frontendUrl", () -> "http://localhost:3000");
        registry.add("wildstore.initialAdmins", () -> "admin@example.com");
        registry.add("wildstore.backupDirectory", () -> System.getProperty("java.io.tmpdir") + "/wildstore-ctrl-backup");
        registry.add("backup.interval", () -> "999999999");
    }
}