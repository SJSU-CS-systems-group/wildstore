package com.sjsu.wildfirestorage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CliTest {
    private static ConfigurableApplicationContext springCtx;
    private static String metaURL;

    @Configuration
    public static class TestOAuthSecurityConfig {
        @Bean
        public ClientRegistrationRepository clientRegistrationRepository() {
            return registrationId -> null;
        }
    }

    @BeforeAll
    public static void setup() throws IOException {
        var sock = new ServerSocket(0);
        var port = sock.getLocalPort();
        sock.close();

        var props = new Properties();
        props.load(ClassLoader.getSystemResourceAsStream("test.properties"));

        var app = new SpringApplication(com.sjsu.wildfirestorage.spring.Main.class, TestOAuthSecurityConfig.class);
        app.setDefaultProperties(props);

        springCtx = app.run("--spring.data.mongodb.uri=mongodb://localhost/wildfire-test-" + System.currentTimeMillis(),
                            "--server.port=" + port );
        while (!springCtx.isActive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        metaURL = "http://localhost:" + port;
    }

    @AfterAll
    public static void teardown() {
        if (springCtx != null) {
            springCtx.getBean(MongoTemplate.class).getDb().drop();
            springCtx.close();
        }

    }

    @Test
    public void contextLoads() {
    }
    @TempDir
    Path tempDir;

    @Test
    public void testUserCli() throws InterruptedException, IOException {
        String role = "ROLE_ADMIN";
        String name = "boss";
        String email = "boss@example.com";
        String token = "bigboss";

        var cmd = new Main.Cli();
        var adminTokenFile = tempDir.resolve("admin-token.txt");

        // the token file doesn't even exist, so this should fail
        var result = clirun(cmd, "user", "list", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        Assertions.assertEquals(2, result.exitCode);
        Assertions.assertTrue(result.err.contains("No such file"));
        Files.write(adminTokenFile, ("token=" + token).getBytes());
        result = clirun(cmd, "user", "list", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        Assertions.assertEquals(1, result.exitCode);

        Assertions.assertTrue(result.err.contains("Unauthorized"));
        springCtx.getBean(MongoTemplate.class).createCollection("userData");
        var rec = Map.of("role", role,
                         "name", name,
                         "email", email,
                         "token", token);
        springCtx.getBean(MongoTemplate.class).insert(new HashMap(rec), "userData");
        result = clirun(cmd, "user", "list", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        Assertions.assertEquals(0, result.exitCode);
        result = clirun(cmd, "user", "update", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        System.out.println(result);

    }
    record TestResult(int exitCode, String out, String err) {}
    private static TestResult clirun(Object command, String... args) {
        var commandLine = new CommandLine(command);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        commandLine.setOut(new PrintWriter(out, true));
        commandLine.setErr(new PrintWriter(err, true));

        int exitCode = commandLine.execute(args);

        return new TestResult(
                exitCode,
                out.toString(),
                err.toString()
        );
    }
}
