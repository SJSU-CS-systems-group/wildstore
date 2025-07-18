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
import picocli.CommandLine;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
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
    public void testCrawl() throws IOException, URISyntaxException, ExecutionException, InterruptedException,
            InvalidRangeException {
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

        var netCDFFileName = tempDir.resolve("testData.nc");
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, netCDFFileName.toString());
        Dimension dim = writer.addDimension(null, "dim", 3);
        Variable var = writer.addVariable(null, "temperature", DataType.FLOAT, "dim");
        writer.addVariableAttribute(var, new Attribute("units", "celsius"));
        writer.create();
        ArrayFloat.D1 data = new ArrayFloat.D1(3);
        data.set(0, 10.0f);
        data.set(1, 20.0f);
        data.set(2, 30.0f);
        writer.write(var, data);

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

        // create a file with the filenames to crawl
        var nameFile = tempNameDir.resolve("fileNames.txt");
        var numToCrawl = Math.min(20, fileNames.size());
        Files.write(nameFile, String.join("\n", fileNames.subList(0, numToCrawl)).getBytes());

        //create a file with the directory to crawl
        var dirFile = tempNameDir.resolve("dirName.txt");
        Files.write(dirFile, tempDir.toAbsolutePath().toString().getBytes());

        //create a file with test data to crawl
        var testFile = tempNameDir.resolve("testName.txt");
        Files.write(testFile, netCDFFileName.toAbsolutePath().toString().getBytes());

        // crawl method test
        WildfireFilesCrawler.crawl(fileNames.get(0), Client.getWebClient(metaURL + "/api/metadata", userToken), 1024 * 1024, "all", false);

        // guests should not be able to crawl
        var result = clirun(WildfireFilesCrawler.class,
                "--metaURL", metaURL,
                "--tokenFile", guestTokenFile.toString(), nameFile.toString());
        Assertions.assertEquals(1, result.exitCode);
        Assertions.assertTrue(result.err.contains("WebClientResponseException$Forbidden"));

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
        Assertions.assertTrue(result.err.contains("WebClientResponseException$Unauthorized"));

        // user should be able to crawl
        result = clirun(WildfireFilesCrawler.class,
                        "--metaURL", metaURL,
                        "--tokenFile", userTokenFile.toString(), nameFile.toString());
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertTrue(result.out.contains("Successfully processed file"));
        Assertions.assertTrue(result.out.contains((numToCrawl - 1) + " valid files found."));
        Assertions.assertTrue(result.out.contains("Crawled " + (numToCrawl - 1) + " new files."));
        Assertions.assertTrue(result.out.contains("Skipped 1 files already crawled."));

        // should be able to crawl all files found in a directory
        result = clirun(WildfireFilesCrawler.class,
                        "--metaURL", metaURL,
                        "--tokenFile", userTokenFile.toString(), dirFile.toString());
        Assertions.assertEquals(0, result.exitCode);
        var expected = Files.walk(tempDir)
                .filter(path -> path.toString().endsWith(".nc"))
                .count();
        Assertions.assertTrue(result.out.contains((expected - numToCrawl) + " valid files found."));
        Assertions.assertTrue(result.out.contains("Crawled " + (expected - numToCrawl - 1) + " new files.")); // -1 because there is a duplicate file
        Assertions.assertTrue(result.out.contains("Skipped " + numToCrawl + " files already crawled."));

        // should crawl files with a later lastModified date
        var metaDataCount = springCtx.getBean(MongoTemplate.class).getCollection("metadata").countDocuments();
        long originalTime = Files.getLastModifiedTime(netCDFFileName).toMillis();
        writer.write(var, data);
        writer.close();
        long updatedTime = Files.getLastModifiedTime(netCDFFileName).toMillis();
        Assertions.assertTrue(updatedTime > originalTime);

        result = clirun(WildfireFilesCrawler.class,
                        "--metaURL", metaURL,
                        "--tokenFile", userTokenFile.toString(), testFile.toString());
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertTrue(result.out.contains("1 valid files found."));
        Assertions.assertTrue(result.out.contains("Crawled 1 new files."));
        Assertions.assertTrue(result.out.contains("Skipped 0 files already crawled."));
        Assertions.assertEquals(metaDataCount, springCtx.getBean(MongoTemplate.class).getCollection("metadata").countDocuments());

        // shouldn't crawl new files
        result = clirun(WildfireFilesCrawler.class,
                        "--metaURL", metaURL,
                        "--tokenFile", userTokenFile.toString(), dirFile.toString());
        System.out.println(result);
        System.out.println("Expected: " + expected);
        Assertions.assertTrue(result.out.contains("0 valid files found."));
        Assertions.assertTrue(result.out.contains("Crawled 0 new files."));
        Assertions.assertTrue(result.out.contains("Skipped " + expected + " files already crawled."));
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
