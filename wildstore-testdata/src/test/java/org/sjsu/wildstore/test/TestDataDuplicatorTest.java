package org.sjsu.wildstore.test;

import edu.sjsu.wildstore.TestDataDuplicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestDataDuplicatorTest {

    @Test
    void maxCountIs1440() {
        assertEquals(24 * 60, TestDataDuplicator.MAX_COUNT);
    }

    @Test
    void testDuplicateFiles(@TempDir Path tmpDir) throws IOException {
        // plain text: checks count, existence, and time increments including hour rollover
        var source = writeFile(tmpDir, "source.nc", "00:00Z");
        var outputs = captureOutputs(source, 65, tmpDir);

        assertEquals(65, outputs.size());
        for (var f : outputs) assertTrue(f.exists());
        assertEquals("00:00Z", Files.readString(outputs.get(0).toPath()));
        assertEquals("00:01Z", Files.readString(outputs.get(1).toPath()));
        assertEquals("00:59Z", Files.readString(outputs.get(59).toPath()));
        assertEquals("01:00Z", Files.readString(outputs.get(60).toPath()));

        // binary data: time pattern surrounded by non-text bytes is still found and updated
        byte[] binaryContent = {0x01, 0x02, '0', '0', ':', '0', '0', 0x03};
        var binarySource = tmpDir.resolve("binary.nc").toFile();
        Files.write(binarySource.toPath(), binaryContent);
        var binaryOutputs = captureOutputs(binarySource, 61, tmpDir);
        byte[] result60 = Files.readAllBytes(binaryOutputs.get(60).toPath());
        assertEquals('0', result60[2]);
        assertEquals('1', result60[3]);
        assertEquals('0', result60[5]);
        assertEquals('0', result60[6]);

        var noTimeSource = writeFile(tmpDir, "notime.nc", "no time pattern here");
        assertThrows(IOException.class, () ->
            TestDataDuplicator.duplicateFiles(noTimeSource, 1, (f, i) -> tmpDir.resolve("out.nc").toFile())
        );
    }

    @Test
    void testCreatesFiles(@TempDir Path tmpDir) throws Exception {
        var source = writeFile(tmpDir, "data.nc", "time: 00:00 end");

        var dup = new TestDataDuplicator();
        setField(dup, "fileToDuplicate", source);
        setField(dup, "count", 3);
        dup.run();

        assertTrue(tmpDir.resolve("data-0.nc").toFile().exists());
        assertTrue(tmpDir.resolve("data-1.nc").toFile().exists());
        assertTrue(tmpDir.resolve("data-2.nc").toFile().exists());
    }

    // helpers

    private File writeFile(Path dir, String name, String content) throws IOException {
        var f = dir.resolve(name).toFile();
        Files.write(f.toPath(), content.getBytes());
        return f;
    }

    private List<File> captureOutputs(File source, int count, Path tmpDir) throws IOException {
        List<File> outputs = new ArrayList<>();
        TestDataDuplicator.duplicateFiles(source, count, (f, i) -> {
            var out = tmpDir.resolve("out-" + i + ".nc").toFile();
            outputs.add(out);
            return out;
        });
        return outputs;
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}