package edu.sjsu.wildstore;

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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static java.lang.String.format;

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

        var app = new SpringApplication(edu.sjsu.wildstore.meta.Main.class, TestOAuthSecurityConfig.class);
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
        springCtx.getBean(MongoTemplate.class).createCollection("userData");
        springCtx.getBean(MongoTemplate.class).createCollection("metadata");
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
        var userTokenFile = tempDir.resolve("user-token.txt");

        // the token file doesn't even exist, so this should fail with no such file
        var result = clirun(cmd, "user", "list", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        Assertions.assertEquals(2, result.exitCode);
        Assertions.assertTrue(result.err.contains("No such file"));

        // create the token file but since it's not in the db, it should give unauthorized
        Files.write(adminTokenFile, ("token=" + token).getBytes());
        result = clirun(cmd, "user", "list", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        Assertions.assertEquals(1, result.exitCode);
        Assertions.assertTrue(result.err.contains("Unauthorized"), format("%s doesn't contain Unauthorized", result.err));

        createUser(role, name, email, token);
        result = clirun(cmd, "user", "list", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertEquals("", result.err);
        Assertions.assertEquals(format("%s: %s %s%n", email, role, name), result.out);

        result = clirun(cmd, "user", "update", "x@y.z", "--role", "user", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertEquals("", result.err);
        Assertions.assertEquals("User x@y.z updated to role ROLE_USER\n", result.out);

        result = clirun(cmd, "user", "getToken", "x@y.z", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertEquals("", result.err);
        var userToken = result.out.trim().split(" ")[1];

        Files.write(userTokenFile, ("token=" + userToken).getBytes());
        result = clirun(cmd, "user", "getToken", "x@y.z", "--metaURL", metaURL, "--token", userTokenFile.toString());
        Assertions.assertEquals(1, result.exitCode);
        Assertions.assertTrue(result.err.contains("Forbidden"));

        result = clirun(cmd, "user", "update", "x2@y.z", "--metaURL", metaURL, "--token", userTokenFile.toString());
        Assertions.assertEquals(1, result.exitCode);
        Assertions.assertTrue(result.err.contains("Forbidden"));

        result = clirun(cmd, "user", "list", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertEquals("", result.err);
        Assertions.assertEquals(2, result.out.split("\n").length);

        result = clirun(cmd, "user", "remove", "x@y.z", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertEquals("", result.err);

        result = clirun(cmd, "user", "list", "--metaURL", metaURL, "--token", adminTokenFile.toString());
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertEquals("", result.err);
        Assertions.assertEquals(1, result.out.split("\n").length);

        deleteUsers();
    }

    @Test
    void testShare() throws IOException {
        var cmd = new Main.Cli();
        var userTokenFile = tempDir.resolve("user-token.txt");
        var guestTokenFile = tempDir.resolve("guest-token.txt");
        var testDataFile = tempDir.resolve("test-data.txt");
        var testDataFile2 = tempDir.resolve("test-data-2.txt");
        var userToken = "secret-user-token";
        var guestToken = "secret-guest-token";

        Files.write(userTokenFile, ("token=" + userToken).getBytes());
        Files.write(guestTokenFile, ("token=" + guestToken).getBytes());
        Files.write(testDataFile, "dummy content".getBytes());

        var testDataPath = testDataFile.toAbsolutePath();
        var testDataPath2 = testDataFile2.toAbsolutePath();
        var testMeta = new Metadata();
        var testMeta2 = new Metadata();
        testMeta.fileName = new HashSet<String>(Set.of(testDataFile.toAbsolutePath().toString()));
        testMeta.filePath = new HashSet<String>(Set.of(testDataFile.toAbsolutePath().getParent().toString()));
        testMeta.digestString = "dummy-digest";
        testMeta2.fileName = new HashSet<String>(Set.of(testDataPath2.toAbsolutePath().toString()));
        testMeta2.filePath = new HashSet<String>(Set.of(testDataPath2.toAbsolutePath().getParent().toString()));
        testMeta2.digestString = "dummy-digest-2";
        springCtx.getBean(MongoTemplate.class).insert(testMeta, "metadata");
        springCtx.getBean(MongoTemplate.class).insert(testMeta2, "metadata");

        createUser("ROLE_USER", "ShareUser", "user@share", userToken);
        createUser("ROLE_GUEST", "ShareGuest", "guest@share", guestToken);

        // guests should not be able to share
        var result = clirun(cmd, "share", testDataPath.toString(), "--metaURL", metaURL, "--token", guestTokenFile.toString(), "--email", "guest@share");
        Assertions.assertEquals(1, result.exitCode);
        Assertions.assertTrue(result.err.contains("CommandLine$ExecutionException"));

        // users need to provide the required parameters
        result = clirun(cmd, "share", testDataPath.toString(), "--metaURL", metaURL, "--token", userTokenFile.toString());
        Assertions.assertEquals(2, result.exitCode);
        Assertions.assertTrue(result.err.contains("Missing required option"));

        result = clirun(cmd, "share", testDataPath.toString(), "--metaURL", metaURL, "--email", "user@share");
        Assertions.assertEquals(2, result.exitCode);
        Assertions.assertTrue(result.err.contains("Missing required option"));

        result = clirun(cmd, "share", testDataPath.toString(), "--metaURL", "--token", userTokenFile.toString(), "--email", "user@share");
        Assertions.assertEquals(2, result.exitCode);
        Assertions.assertTrue(result.err.contains("Expected parameter for option '--metaURL'"));

        result = clirun(cmd, "share", testDataPath.toString(), "--metaURL", metaURL, "--token", "--email", "user@share");
        Assertions.assertEquals(2, result.exitCode);
        Assertions.assertTrue(result.err.contains("Expected parameter for option '--token'"));

        // users need to provide a valid file
        result = clirun(cmd, "share", "--metaURL", metaURL, "--token", userTokenFile.toString(), "--email", "user@share");
        Assertions.assertEquals(2, result.exitCode);
        Assertions.assertTrue(result.err.contains("Missing required parameter: '<fileNames>'"));

        result = clirun(cmd, "share", "", "--metaURL", metaURL, "--token", userTokenFile.toString(), "--email", "user@share");
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertTrue(result.err.contains("Missing Files"));

        result = clirun(cmd, "share", "/testfile", "--metaURL", metaURL, "--token", userTokenFile.toString(), "--email", "user@share");
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertTrue(result.err.contains("Missing Files"));

        result = clirun(cmd, "share", testDataPath.toString(), "--metaURL", metaURL, "--token", userTokenFile.toString(), "--email", "user@share");
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertTrue(result.out.contains("?filename=test-data.txt"));

        // metaURL needs to be valid
        result = clirun(cmd, "share", testDataPath.toString(), "--metaURL", "http://localhost:", "--token", userTokenFile.toString(), "--email", "user@share");
        Assertions.assertEquals(1, result.exitCode);
        Assertions.assertTrue(result.err.contains("WebClientRequestException"));

        // token needs to be valid
        result = clirun(cmd, "share", testDataPath.toString(), "--metaURL", metaURL, "--token", "faulty-token", "--email", "user@share");
        Assertions.assertEquals(2, result.exitCode);
        Assertions.assertTrue(result.err.contains("No such file or directory"));

        // sharing multiple files
        result = clirun(cmd, "share", testDataPath.toString(), testDataPath2.toString(), "--metaURL", metaURL, "--token", userTokenFile.toString(), "--email", "user@share");
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertTrue(result.out.contains("?filename=test-data.txt") && result.out.contains("?filename=test-data-2.txt") && !result.err.contains("Missing Files"));

        result = clirun(cmd, "share", testDataPath.toString(), "/testfile", "--metaURL", metaURL, "--token", userTokenFile.toString(), "--email", "user@share");
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertTrue(result.out.contains("?filename=test-data.txt") && result.err.contains("/testfile"));

        deleteUsers();
    }

    private static void createUser(String role, String name, String email, String token) {
        // add the token to the db, so everything should work
        var rec = Map.of("role", role,
                         "name", name,
                         "email", email,
                         "token", token);
        springCtx.getBean(MongoTemplate.class).insert(new HashMap(rec), "userData");
    }

    private static void deleteUsers() {
        var query = new Query(Criteria.where("email").regex("@share"));
        springCtx.getBean(MongoTemplate.class).remove(query, "userData");
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
