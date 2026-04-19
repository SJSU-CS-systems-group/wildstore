package edu.sjsu.wildstore;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class PrintDataTest {

    @TempDir
    Path tempDir;

    static Metadata minimalMeta;
    static Metadata fullMeta;

    @BeforeAll
    static void setup() {
        minimalMeta = new Metadata();
        minimalMeta.fileName = Set.of("test.nc");
        minimalMeta.filePath = Set.of("/data/");
        minimalMeta.fileSize = 1024L;
        minimalMeta.domain = 1;

        fullMeta = new Metadata();
        fullMeta.fileName = Set.of("full.nc");
        fullMeta.filePath = Set.of("/data/");
        fullMeta.fileSize = 2048L;
        fullMeta.domain = 2;
        fullMeta.digestString = "abc123";
        fullMeta.fileType = Set.of("netcdf");
        fullMeta.location = new GeoJsonPolygon(List.of(
                new Point(-120, 35), new Point(-110, 35),
                new Point(-110, 40), new Point(-120, 40),
                new Point(-120, 35)));

        var attr = new WildfireAttribute();
        attr.attributeName = "title";
        attr.type = "String";
        attr.value = "test dataset";
        fullMeta.globalAttributes = List.of(attr);

        var v = new WildfireVariable();
        v.variableName = "TEMP";
        v.minValue = 10f;
        v.maxValue = 30f;
        v.average = 20f;
        v.attributeList = List.of();
        v.elementMap = new HashMap<>();
        fullMeta.variables = List.of(v);
    }

    private String capture(Runnable action) {
        var buf = new ByteArrayOutputStream();
        var prev = System.out;
        System.setOut(new PrintStream(buf));
        try {
            action.run();
        } finally {
            System.setOut(prev);
        }
        return buf.toString();
    }

    @Test
    void testPrintAllData() {
        var nullOutput = capture(() -> PrintData.printAllData(minimalMeta));
        Assertions.assertTrue(nullOutput.contains("Corners: Null"));
        Assertions.assertFalse(nullOutput.contains("Attributes\n"));
        Assertions.assertFalse(nullOutput.contains("Variables\n"));

        var fullOutput = capture(() -> PrintData.printAllData(fullMeta));
        Assertions.assertFalse(fullOutput.contains("Corners: Null"));
        Assertions.assertTrue(fullOutput.contains("Attributes"));
        Assertions.assertTrue(fullOutput.contains("title"));
        Assertions.assertTrue(fullOutput.contains("Variables"));
        Assertions.assertTrue(fullOutput.contains("TEMP"));
    }

    @Test
    void testPrintBasic() {
        var nullOutput = capture(() -> PrintData.printBasic(minimalMeta));
        Assertions.assertTrue(nullOutput.contains("Corners: Null"));
        Assertions.assertFalse(nullOutput.contains("Attributes:"));
        Assertions.assertFalse(nullOutput.contains("Variables:"));

        var fullOutput = capture(() -> PrintData.printBasic(fullMeta));
        Assertions.assertFalse(fullOutput.contains("Corners: Null"));
        Assertions.assertTrue(fullOutput.contains("Attributes:"));
        Assertions.assertTrue(fullOutput.contains("title"));
        Assertions.assertTrue(fullOutput.contains("Variables:"));
        Assertions.assertTrue(fullOutput.contains("TEMP"));
    }

    @Test
    void testPrintEnums() throws IOException {
        Path enumFile = tempDir.resolve("enums.txt");
        Files.writeString(enumFile, "EXISTING_VAR\n");

        var newVar = new WildfireVariable();
        newVar.variableName = "NEW_VAR";
        newVar.elementMap = new HashMap<>();
        newVar.elementMap.put(1f, 0.5f);
        newVar.attributeList = List.of();

        var existing = new WildfireVariable();
        existing.variableName = "EXISTING_VAR";
        existing.elementMap = new HashMap<>();
        existing.elementMap.put(1f, 0.5f);
        existing.attributeList = List.of();

        var emptyMap = new WildfireVariable();
        emptyMap.variableName = "EMPTY_MAP";
        emptyMap.elementMap = new HashMap<>();
        emptyMap.attributeList = List.of();

        var m = new Metadata();
        m.variables = List.of(newVar, existing, emptyMap);

        PrintData.printEnums(enumFile, m);

        var lines = Files.readAllLines(enumFile);
        Assertions.assertTrue(lines.contains("NEW_VAR"));
        Assertions.assertEquals(2, lines.size()); // EXISTING_VAR + NEW_VAR only
        Assertions.assertFalse(lines.contains("EMPTY_MAP"));

        Assertions.assertThrows(RuntimeException.class,
                () -> PrintData.printEnums(Path.of("nonexistent/enums.txt"), m));
    }
}