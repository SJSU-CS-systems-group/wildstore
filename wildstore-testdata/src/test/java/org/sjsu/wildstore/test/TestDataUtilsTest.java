package org.sjsu.wildstore.test;

import edu.sjsu.wildstore.TestDataUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class TestDataUtilsTest {

    @Test
    void testGetFileSystemForUrl(@TempDir Path tmpDir) throws Exception {
        var fsMethod = TestDataUtils.class.getDeclaredMethod("getFileSystemForUrl", URL.class);
        fsMethod.setAccessible(true);

        // file: protocol → default file system
        assertSame(FileSystems.getDefault(), fsMethod.invoke(null, tmpDir.toUri().toURL()));

        // jar: protocol → a distinct, new file system
        var jarUrl = new URL("jar:" + createTestJar(tmpDir).toUri() + "!/dummy.txt");
        FileSystem jarFs = (FileSystem) fsMethod.invoke(null, jarUrl);
        assertNotNull(jarFs);
        assertNotSame(FileSystems.getDefault(), jarFs);
        jarFs.close();
    }

    @Test
    void testGetPathForUrl(@TempDir Path tmpDir) throws Exception {
        var pathMethod = TestDataUtils.class.getDeclaredMethod("getPathForUrl", FileSystem.class, URL.class);
        pathMethod.setAccessible(true);

        // file: protocol → path on the default FS
        var file = tmpDir.resolve("test.nc");
        assertEquals(file, pathMethod.invoke(null, FileSystems.getDefault(), file.toUri().toURL()));

        // jar: protocol → internal path extracted after "!"
        var jarPath = createTestJar(tmpDir);
        var jarUrl = new URL("jar:" + jarPath.toUri() + "!/internal/file.txt");
        try (var jarFs = FileSystems.newFileSystem(jarPath)) {
            var internalPath = (Path) pathMethod.invoke(null, jarFs, jarUrl);
            assertEquals("/internal/file.txt", internalPath.toString());
        }
    }

    @Test
    void testExtractTestData(@TempDir Path tmpDir) throws Exception {
        // Load TestDataUtils into a ClassLoader that has no testdata.zip resource
        copyClassFileTo(TestDataUtils.class, tmpDir);
        try (var cl = new URLClassLoader(new URL[]{tmpDir.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            var method = cl.loadClass("edu.sjsu.wildstore.TestDataUtils").getMethod("extractTestData", Path.class);
            var outDir = Files.createDirectories(tmpDir.resolve("out"));
            var ex = assertThrows(InvocationTargetException.class, () -> method.invoke(null, outDir));
            assertInstanceOf(IOException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("testdata.zip"));
        }

        // Create a testdata.zip whose entry name starts with "/"
        try (var zos = new ZipOutputStream(Files.newOutputStream(tmpDir.resolve("testdata.zip")))) {
            zos.putNextEntry(new ZipEntry("/prefixed/file.txt"));
            zos.write("content".getBytes());
            zos.closeEntry();
        }
        copyClassFileTo(TestDataUtils.class, tmpDir);
        try (var cl = new URLClassLoader(new URL[]{tmpDir.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            var method = cl.loadClass("edu.sjsu.wildstore.TestDataUtils").getMethod("extractTestData", Path.class);
            var outDir = Files.createDirectories(tmpDir.resolve("out"));
            method.invoke(null, outDir);
            // leading "/" must be stripped so the path resolves correctly
            assertTrue(outDir.resolve("prefixed/file.txt").toFile().exists());
        }
    }

    // helpers

    private Path createTestJar(Path tmpDir) throws IOException {
        var jarPath = tmpDir.resolve("test.jar");
        if (!jarPath.toFile().exists()) {
            try (var jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
                jos.putNextEntry(new JarEntry("dummy.txt"));
                jos.write("hello".getBytes());
                jos.closeEntry();
            }
        }
        return jarPath;
    }

    private void copyClassFileTo(Class<?> clazz, Path dir) throws IOException {
        var resource = "/" + clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getResourceAsStream(resource)) {
            assertNotNull(is, "Class resource not found: " + resource);
            var dest = dir.resolve(clazz.getName().replace('.', '/') + ".class");
            Files.createDirectories(dest.getParent());
            Files.write(dest, is.readAllBytes());
        }
    }
}