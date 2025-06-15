package com.sjsu.wildfirestorage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
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

    @TestConfiguration
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
        var cmd = new Main.Cli();
        var adminTokenFile = tempDir.resolve("admin-token.txt");
        var result = clirun(cmd, "user", "list", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        System.out.println(result);
        Assertions.assertEquals(2, result.exitCode);
        Assertions.assertTrue(result.err.contains("No such file"));
        Files.write(adminTokenFile, "token=bigboss".getBytes());
        result = clirun(cmd, "user", "list", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        System.out.println("------------------");
        System.out.println(result);
        springCtx.getBean(MongoTemplate.class).createCollection("userData");
        var rec = Map.of("role", "ROLE_ADMIN",
                         "name", "boss",
                         "email", "boss@example.com",
                         "token", "bigboss");
        springCtx.getBean(MongoTemplate.class).insert(new HashMap(rec), "userData");
        result = clirun(cmd, "user", "list", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        System.out.println(result);
        Thread.sleep(10000);
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
