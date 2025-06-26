package org.sjsu.wildstore.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarOutputStream;

public class JarAccessTest {
    @Test
    void testJarAccess(@TempDir Path tmpDir) throws Exception {
        var testPath = tmpDir.resolve("test.jar");
        try (var jarOutputStream = new JarOutputStream(Files.newOutputStream(testPath))) {
            jarOutputStream.putNextEntry(new java.util.jar.JarEntry("test.txt"));
            jarOutputStream.write("Hello, World!".getBytes());
            jarOutputStream.closeEntry();
        }
        var testURI = testPath.toUri();
        System.out.println(testURI);
        var uri = new URI("jar:" + testURI.getSchemeSpecificPart() + "!/test.txt");
        var fs = FileSystems.newFileSystem(new URI("jar:" + testURI.toString()), Map.of());
        var path = fs.getPath("/test.txt");
        System.out.println("Accessing file in jar: " + path.toUri());
        var content = Files.readString(path);
        System.out.println("Content of test.txt: " + content);
    }
}
