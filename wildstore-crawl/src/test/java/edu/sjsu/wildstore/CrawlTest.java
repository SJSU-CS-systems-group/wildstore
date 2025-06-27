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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.reactive.function.client.WebClient;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class CrawlTest {
    private static ConfigurableApplicationContext springCtx;
    private static String metaURL;

    @TempDir
    static Path tempDir;


    @Configuration
    public static class TestOAuthSecurityConfig {
        @Bean
        public ClientRegistrationRepository clientRegistrationRepository() {
            return registrationId -> null;
        }
    }

    @BeforeAll
    public static void setup() throws Exception {
        TestDataUtils.extractTestData(tempDir);
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
    static Path tempNameDir;

    @Test
    public void testCrawl() throws IOException, URISyntaxException {
        String userToken = "secret-user-token";
        String guestToken = "secret-guest-token";
        createUser("ROLE_USER", "user", "user@crawl", userToken);
        createUser("ROLE_GUEST", "guest", "guest@crawl", guestToken);

        var userTokenFile = tempDir.resolve("user-token.txt");
        var guestTokenFile = tempDir.resolve("guest-token.txt");
        var faultyTokenFile = tempDir.resolve("faulty-token.txt");
        Files.write(userTokenFile, ("token=" + userToken).getBytes());
        Files.write(guestTokenFile, ("token=" + guestToken).getBytes());
        Files.write(faultyTokenFile, "token=not-a-valid)-token".getBytes());

        // put files into directory
        var testDataUtils = new TestDataUtils();
        testDataUtils.extractTestData(tempDir);

        // find all .nc files in the tempDir
        List<String> fileNames = List.of();
        try (Stream<Path> walk = Files.walk(tempDir)) {
            fileNames = walk.filter(path -> path.toString().endsWith(".nc")).map(Path::toString).toList();
        }
        if (fileNames.isEmpty()) {
            throw new IOException("No .nc files found in the test data directory.");
        }

        // create a file with the names of the files
        var nameFile = tempNameDir.resolve("fileNames.txt");
        Files.write(nameFile, String.join("\n", fileNames.subList(0, Math.min(20, fileNames.size()))).getBytes());
        try {
            WildfireFilesCrawler.crawl(fileNames.get(1), Client.getWebClient(metaURL + "/api/metadata"), userToken, 1024 * 1024, "all", false);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // guests should not be able to crawl
        var result = clirun(WildfireFilesCrawler.class,
                "--metaURL", metaURL,
                "--tokenFile", guestTokenFile.toString(), nameFile.toString());
        Assertions.assertEquals(1, result.exitCode);
        Assertions.assertTrue(result.err.contains("CommandLine$ExecutionException") && result.err.contains("Error processing file"));

        // faulty tokenFile test
        result = clirun(WildfireFilesCrawler.class,
                        "--metaURL", metaURL,
                        "--tokenFile", "faulty-token", nameFile.toString());

        Assertions.assertEquals(1, result.exitCode);
        Assertions.assertTrue(result.err.contains("CommandLine$PicocliException") && result.err.contains("is not a valid file"));

        // faulty token test
        result = clirun(WildfireFilesCrawler.class,
                        "--metaURL", metaURL,
                        "--tokenFile", faultyTokenFile.toString(), nameFile.toString());
        Assertions.assertEquals(1, result.exitCode);
        Assertions.assertTrue(result.err.contains("CommandLine$ExecutionException: Error(s) occurred during processing."));

        // user should be able to crawl
        result = clirun(WildfireFilesCrawler.class,
                        "--metaURL", metaURL,
                        "--tokenFile", userTokenFile.toString(), nameFile.toString());
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertTrue(result.out.contains("Successfully processed file"));
    }

    private static void createUser(String role, String name, String email, String token) {
        // add the token to the db, so everything should work
        var rec = Map.of("role", role,
                         "name", name,
                         "email", email,
                         "token", token);
        springCtx.getBean(MongoTemplate.class).insert(new HashMap(rec), "userData");
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
