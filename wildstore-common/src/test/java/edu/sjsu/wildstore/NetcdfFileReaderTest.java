package edu.sjsu.wildstore;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;

public class NetcdfFileReaderTest {

    @TempDir
    static Path tempDir;

    private static Path testNcFile;

    @BeforeAll
    static void setup() throws IOException {
        testNcFile = tempDir.resolve("wrfout.nc");
        try (InputStream is = NetcdfFileReaderTest.class.getResourceAsStream("/wrfout.nc");
             var out = Files.newOutputStream(testNcFile)) {
            Assertions.assertNotNull(is, "wrfout.nc test resource not found");
            is.transferTo(out);
        }
    }

    @Test
    void testProcessMetadata() {
        // missing file, should throw RuntimeException error
        Assertions.assertThrows(RuntimeException.class,
                () -> new NetcdfFileReader("nonexistent/path.nc").processMetadata());

        var reader = new NetcdfFileReader(testNcFile.toString());
        var metadata = reader.processMetadata();
        Assertions.assertFalse(metadata.fileName.isEmpty());
        Assertions.assertFalse(metadata.filePath.isEmpty());
        Assertions.assertTrue(metadata.fileSize > 0);
        Assertions.assertTrue(metadata.lastModified > 0);
        Assertions.assertNotNull(reader.getRandomAccessFile());
    }

    @Test
    void testReadGlobalAttributes() throws Exception {
        var path = tempDir.resolve("process_attrs.nc").toString();
        NetcdfFileWriter w = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, path);
        try {
            Dimension d = w.addDimension(null, "x", 1);
            w.addVariable(null, "v", DataType.FLOAT, List.of(d));
            w.addGroupAttribute(null, new Attribute("float_attr", 3.14f));
            w.addGroupAttribute(null, new Attribute("char_attr", "hello"));
            w.addGroupAttribute(null, new Attribute("int_attr", 42));
            w.create();
            w.write(w.findVariable("v"), Array.factory(DataType.FLOAT, new int[]{1}, new float[]{0f}));
        } finally {
            w.close();
        }

        var reader = new NetcdfFileReader(path);
        reader.processMetadata();
        var attrs = reader.readGlobalAttributes();

        var floatAttr = attrs.stream().filter(a -> a.attributeName.equals("float_attr")).findFirst().orElse(null);
        Assertions.assertNotNull(floatAttr);
        Assertions.assertEquals("float", floatAttr.type);
        Assertions.assertNotNull(floatAttr.value);

        var charAttr = attrs.stream().filter(a -> a.attributeName.equals("char_attr")).findFirst().orElse(null);
        Assertions.assertNotNull(charAttr);
        Assertions.assertEquals("String", charAttr.type);
        Assertions.assertNotNull(charAttr.value);

        var intAttr = attrs.stream().filter(a -> a.attributeName.equals("int_attr")).findFirst().orElse(null);
        Assertions.assertNotNull(intAttr);
        Assertions.assertEquals("int", intAttr.type);
        Assertions.assertNotNull(intAttr.value);
    }

    @Test
    void testReadVariables() throws Exception {
        var path = tempDir.resolve("vars.nc").toString();
        NetcdfFileWriter w = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, path);
        try {
            Dimension x = w.addDimension(null, "x", 10);
            Dimension y = w.addDimension(null, "y", 5);
            Dimension c = w.addDimension(null, "c", 19);

            // intEnum: 5 distinct integer values, expect: elementMap populated (2 < 5 < 20, not char, not fractional)
            w.addVariable(null, "intEnum", DataType.FLOAT, List.of(x));

            // frac: fractional values → hasFractionalValue = true, expect: elementMap empty
            w.addVariable(null, "frac", DataType.FLOAT, List.of(y));

            // withAttrs: has _FillValue and missing_value, expect: fill/missing excluded from stats
            var withAttrsVar = w.addVariable(null, "withAttrs", DataType.FLOAT, List.of(x));
            withAttrsVar.addAttribute(new Attribute("_FillValue", 9999f));
            withAttrsVar.addAttribute(new Attribute("missing_value", -9999f));

            // charVar: char type, expect: elementMap empty regardless of unique count
            w.addVariable(null, "charVar", DataType.CHAR, List.of(x, c));

            w.create();

            w.write(w.findVariable("intEnum"), Array.factory(DataType.FLOAT, new int[]{10},
                    new float[]{1f, 2f, 3f, 4f, 5f, 1f, 2f, 3f, 4f, 5f}));
            w.write(w.findVariable("frac"), Array.factory(DataType.FLOAT, new int[]{5},
                    new float[]{1.1f, 2.2f, 3.3f, 4.4f, 5.5f}));
            w.write(w.findVariable("withAttrs"), Array.factory(DataType.FLOAT, new int[]{10},
                    new float[]{1f, 2f, 9999f, -9999f, 3f, 4f, 5f, 9999f, -9999f, 6f}));
            w.write(w.findVariable("charVar"), Array.factory(DataType.CHAR, new int[]{10, 19}, new char[10 * 19]));
        } finally {
            w.close();
        }

        var reader = new NetcdfFileReader(path);
        reader.processMetadata();
        var vars = reader.readVariables();

        // all 4 variables present with non-null variableName, type, varDimensionList, attributeList
        Assertions.assertEquals(4, vars.size());
        Assertions.assertTrue(vars.stream().allMatch(v -> v.variableName != null));
        Assertions.assertTrue(vars.stream().allMatch(v -> v.type != null));
        Assertions.assertTrue(vars.stream().allMatch(v -> v.varDimensionList != null));
        Assertions.assertTrue(vars.stream().allMatch(v -> v.attributeList != null));

        var intEnumResult = vars.stream().filter(v -> v.variableName.equals("intEnum")).findFirst().orElseThrow();
        Assertions.assertFalse(intEnumResult.elementMap.isEmpty());

        var fracResult = vars.stream().filter(v -> v.variableName.equals("frac")).findFirst().orElseThrow();
        Assertions.assertTrue(fracResult.elementMap.isEmpty());

        var attrsResult = vars.stream().filter(v -> v.variableName.equals("withAttrs")).findFirst().orElseThrow();
        Assertions.assertEquals(1f, attrsResult.minValue, 0.001f);
        Assertions.assertEquals(6f, attrsResult.maxValue, 0.001f);
        Assertions.assertTrue(attrsResult.attributeList.stream()
                .anyMatch(a -> a.attributeName.equalsIgnoreCase("_FillValue")));

        var charResult = vars.stream().filter(v -> v.variableName.equals("charVar")).findFirst().orElseThrow();
        Assertions.assertEquals(DataType.CHAR, charResult.type);
        Assertions.assertTrue(charResult.elementMap.isEmpty());

        var xDim = intEnumResult.varDimensionList.stream()
                .filter(d -> d.name().equals("x")).findFirst().orElseThrow();
        Assertions.assertEquals(10, xDim.value());
    }

    @Test
    void testFloatRange() {
        // All valid values: input [1,2,3,4,5], no fill/missing, expect: min=1, max=5, count=5, 5 unique keys
        float[][] r = NetcdfFileReader.floatRange(
                Array.factory(DataType.FLOAT, new int[]{5}, new float[]{1f, 2f, 3f, 4f, 5f}),
                Float.MAX_VALUE, Float.MAX_VALUE);
        Assertions.assertEquals(1f, r[0][0], 0.001f);
        Assertions.assertEquals(5f, r[0][1], 0.001f);
        Assertions.assertEquals(5f, r[0][3], 0.001f);
        Assertions.assertEquals(5,  r[1].length);

        // Fill and missing values excluded: input [1, 9999(fill), 2, -9999(missing), 3], expect: min=1, max=3, 3 unique keys
        float[][] rf = NetcdfFileReader.floatRange(
                Array.factory(DataType.FLOAT, new int[]{5}, new float[]{1f, 9999f, 2f, -9999f, 3f}),
                9999f, -9999f);
        Assertions.assertEquals(1f, rf[0][0], 0.001f);
        Assertions.assertEquals(3f, rf[0][1], 0.001f);
        Assertions.assertEquals(3,  rf[1].length);

        // All fill values: input [9999, 9999, 9999], expect: empty keys array and empty values array
        float[][] ra = NetcdfFileReader.floatRange(
                Array.factory(DataType.FLOAT, new int[]{3}, new float[]{9999f, 9999f, 9999f}),
                9999f, 9999f);
        Assertions.assertEquals(0, ra[1].length);
        Assertions.assertEquals(0, ra[2].length);

        // Single element: input [42] → expected min=42, max=42, expect: 1 unique key
        float[][] rs = NetcdfFileReader.floatRange(
                Array.factory(DataType.FLOAT, new int[]{1}, new float[]{42f}),
                Float.MAX_VALUE, Float.MAX_VALUE);
        Assertions.assertEquals(42f, rs[0][0], 0.001f);
        Assertions.assertEquals(42f, rs[0][1], 0.001f);
        Assertions.assertEquals(1,   rs[1].length);

        // Exactly 20 unique values (at threshold), expect: 20-element key and value arrays
        float[] exact = new float[20];
        for (int i = 0; i < 20; i++) exact[i] = i;
        float[][] re = NetcdfFileReader.floatRange(
                Array.factory(DataType.FLOAT, new int[]{20}, exact),
                Float.MAX_VALUE, Float.MAX_VALUE);
        Assertions.assertEquals(20, re[1].length);
        Assertions.assertEquals(20, re[2].length);

        // More than 20 unique values (exceeds threshold), expect: 21-element placeholder arrays
        float[] over = new float[25];
        for (int i = 0; i < 25; i++) over[i] = i + 0.5f;
        float[][] ro = NetcdfFileReader.floatRange(
                Array.factory(DataType.FLOAT, new int[]{25}, over),
                Float.MAX_VALUE, Float.MAX_VALUE);
        Assertions.assertEquals(21, ro[1].length);
        Assertions.assertEquals(21, ro[2].length);

        // Percentage values: input [1,1,2,2,3], expect: 3 unique keys, each value entry non-negative and sum ≤ 1
        float[][] rp = NetcdfFileReader.floatRange(
                Array.factory(DataType.FLOAT, new int[]{5}, new float[]{1f, 1f, 2f, 2f, 3f}),
                Float.MAX_VALUE, Float.MAX_VALUE);
        Assertions.assertEquals(3, rp[1].length);
        float sum = 0;
        for (float v : rp[2]) { Assertions.assertTrue(v >= 0); sum += v; }
        Assertions.assertTrue(sum <= 1.001f);
    }

    @Test
    void testProcessFileContents() throws Exception {
        // wrfout.nc with WRF-pattern filename: expect globalAttributes and variables populated,
        // fileType set, domain=1, digestString non-null, location non-null (XLAT/XLONG path),
        // StartDate/EndDate in globalAttributes from Times variable
        var wrfFile = tempDir.resolve("wrfout_d01_2022_01_01_00_00_00_pfc.nc");
        Files.copy(testNcFile, wrfFile, StandardCopyOption.REPLACE_EXISTING);
        var reader = new NetcdfFileReader(wrfFile.toString());
        var metadata = reader.processMetadata();
        reader.processFileContents(metadata, 1_000_000_000);

        Assertions.assertFalse(metadata.globalAttributes.isEmpty());
        Assertions.assertFalse(metadata.variables.isEmpty());
        Assertions.assertNotNull(metadata.digestString);
        Assertions.assertNotNull(metadata.fileType);
        Assertions.assertEquals(1, metadata.domain);
        Assertions.assertNotNull(metadata.location);
        Assertions.assertTrue(metadata.globalAttributes.stream()
                .anyMatch(a -> a.attributeName.equals("StartDate")));
        Assertions.assertTrue(metadata.globalAttributes.stream()
                .anyMatch(a -> a.attributeName.equals("EndDate")));

        // corner_lats/corner_lons global attributes: input file with 4-element corner arrays, no XLAT/XLONG,
        // expect: location set from corner attribute values (not calculateCorners)
        var cornersPath = tempDir.resolve("pfc_corners.nc").toString();
        NetcdfFileWriter w = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, cornersPath);
        try {
            Dimension d = w.addDimension(null, "x", 1);
            w.addVariable(null, "v", DataType.FLOAT, List.of(d));
            w.addGroupAttribute(null, new Attribute("corner_lats",
                    Array.factory(DataType.FLOAT, new int[]{4}, new float[]{10f, 20f, 30f, 40f})));
            w.addGroupAttribute(null, new Attribute("corner_lons",
                    Array.factory(DataType.FLOAT, new int[]{4}, new float[]{-120f, -110f, -100f, -90f})));
            w.create();
            w.write(w.findVariable("v"), Array.factory(DataType.FLOAT, new int[]{1}, new float[]{0f}));
        } finally {
            w.close();
        }
        var cornerReader = new NetcdfFileReader(cornersPath);
        var cornerMeta = cornerReader.processMetadata();
        cornerReader.processFileContents(cornerMeta, 1_000_000_000);
        Assertions.assertNotNull(cornerMeta.location);

        // No XLAT/XLONG and no corner attributes: input minimal file with one float variable, no spatial metadata
        // expect: location = null
        var nolocPath = tempDir.resolve("pfc_noloc.nc").toString();
        w = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, nolocPath);
        try {
            Dimension d = w.addDimension(null, "x", 2);
            w.addVariable(null, "v", DataType.FLOAT, List.of(d));
            w.create();
            w.write(w.findVariable("v"), Array.factory(DataType.FLOAT, new int[]{2}, new float[]{1f, 2f}));
        } finally {
            w.close();
        }
        var nolocReader = new NetcdfFileReader(nolocPath);
        var nolocMeta = nolocReader.processMetadata();
        nolocReader.processFileContents(nolocMeta, 1_000_000_000);
        Assertions.assertNull(nolocMeta.location);

        // Times absent, date in filename: input minimal file named wrfout_d01_2022_06_15_12_00_00_notimes.nc, no Times variable
        // expect: StartDate attribute added from parsed filename date
        var noTimesPath = tempDir.resolve("wrfout_d01_2022_06_15_12_00_00_notimes.nc").toString();
        w = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, noTimesPath);
        try {
            Dimension d = w.addDimension(null, "x", 1);
            w.addVariable(null, "v", DataType.FLOAT, List.of(d));
            w.create();
            w.write(w.findVariable("v"), Array.factory(DataType.FLOAT, new int[]{1}, new float[]{1f}));
        } finally {
            w.close();
        }
        var noTimesReader = new NetcdfFileReader(noTimesPath);
        var noTimesMeta = noTimesReader.processMetadata();
        noTimesReader.processFileContents(noTimesMeta, 1_000_000_000);
        Assertions.assertTrue(noTimesMeta.globalAttributes.stream()
                .anyMatch(a -> a.attributeName.equals("StartDate")));
    }

    // =========================================================
    // read() / recursiveRead() / summarizedStats():
    // All three are private; exercised via readVariables() and processFile().
    //
    // single-read path (loopTo < 0): input 1D variable [1..5], maxReadSize=default (1e9) →
    //   total elements fit → direct variable.read() → expect min=1, max=5
    //
    // recursive-read path, summarizedStats pass_enum_threshold=true: input 2D variable [2,25],
    //   maxReadSize=1 → loopTo=0 → recursiveRead base case → each chunk has 25 elements > ENUM_THRESHOLD(20) →
    //   expect pass_enum_threshold=true → 21-element placeholder returned → elementMap empty
    //
    // recursive-read path, summarizedStats NPE (pass_enum_threshold=false):
    //   input 2D variable [2,5] with distinct values per row, maxReadSize=1 → two chunks with non-overlapping keys →
    //   uniqueElements.get(key) returns null → NullPointerException → variable still added
    // =========================================================
    @Test
    void testRead() throws Exception {
        // Single read path: 1D variable with values [1,2,3,4,5], default maxReadSize → loopTo < 0 → direct read
        // expect: min=1, max=5 on the resulting WildfireVariable
        var singlePath = tempDir.resolve("read_single.nc").toString();
        NetcdfFileWriter w = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, singlePath);
        try {
            Dimension d = w.addDimension(null, "x", 5);
            w.addVariable(null, "data", DataType.FLOAT, List.of(d));
            w.create();
            w.write(w.findVariable("data"), Array.factory(DataType.FLOAT, new int[]{5}, new float[]{1f, 2f, 3f, 4f, 5f}));
        } finally {
            w.close();
        }
        var singleReader = new NetcdfFileReader(singlePath);
        singleReader.processMetadata();
        var singleVars = singleReader.readVariables();
        var dataVar = singleVars.stream().filter(v -> v.variableName.equals("data")).findFirst().orElseThrow();
        Assertions.assertEquals(1f, dataVar.minValue, 0.001f);
        Assertions.assertEquals(5f, dataVar.maxValue, 0.001f);

        // Recursive path, pass_enum_threshold=true: 2D variable [2,25], maxReadSize=1 → each chunk has 25 elements
        // > ENUM_THRESHOLD(20) → summarizedStats sets pass_enum_threshold=true → 21-element placeholder keys returned
        // → after dedup in readVariables, elementMap is empty
        var recPath = tempDir.resolve("read_recursive.nc").toString();
        w = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, recPath);
        try {
            Dimension rows = w.addDimension(null, "rows", 2);
            Dimension cols = w.addDimension(null, "cols", 25);
            w.addVariable(null, "grid", DataType.FLOAT, List.of(rows, cols));
            w.create();
            float[] gridData = new float[50];
            for (int i = 0; i < 50; i++) gridData[i] = i;
            w.write(w.findVariable("grid"), Array.factory(DataType.FLOAT, new int[]{2, 25}, gridData));
        } finally {
            w.close();
        }
        var recReader = new NetcdfFileReader(recPath);
        var recMeta = recReader.processMetadata();
        recReader.processFileContents(recMeta, 1); // maxReadSize=1 forces recursive read
        var gridVar = recMeta.variables.stream().filter(v -> v.variableName.equals("grid")).findFirst().orElseThrow();
        Assertions.assertTrue(gridVar.elementMap.isEmpty()); // placeholder path → empty elementMap

        // Recursive path, NPE in summarizedStats:
        // 2D variable [2,5] with distinct values per row ([1..5] and [6..10]), maxReadSize=1 →
        // recursiveRead produces two chunks with non-overlapping keys →
        // summarizedStats uniqueElements.get(key) returns null for unseen keys → NullPointerException
        //  → variable still added to list despite exception
        var npePath = tempDir.resolve("read_npe.nc").toString();
        w = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, npePath);
        try {
            Dimension rows = w.addDimension(null, "rows", 2);
            Dimension cols = w.addDimension(null, "cols", 5);
            w.addVariable(null, "npe", DataType.FLOAT, List.of(rows, cols));
            w.create();
            w.write(w.findVariable("npe"), Array.factory(DataType.FLOAT, new int[]{2, 5},
                    new float[]{1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f}));
        } finally {
            w.close();
        }
        var npeMeta = new NetcdfFileReader(npePath).processFile(1);
        var npeVar = npeMeta.variables.stream().filter(v -> v.variableName.equals("npe")).findFirst().orElse(null);
        Assertions.assertNotNull(npeVar); // variable still added despite NPE in summarizedStats
    }

    // =========================================================
    // findNFUEL_CAT_BURNT():
    // Private; exercised via processFileContents() with synthetic FIRE_AREA + NFUEL_CAT variables.
    //
    // fire > 0 in last time step, multiple fuel categories: input FIRE_AREA[Time=2,3,3] with indices 9-11 > 0,
    //   NFUEL_CAT values {1,2,3} at those positions → expect NFUEL_CAT_BURNT added, elementMap non-empty, average > 0
    //
    // all fire_area = 0: input all zeros → expect nfuel_counts empty → NFUEL_CAT_BURNT not added
    //
    // fill value exclusion: input FIRE_AREA with _FillValue=9999f, fire positions set to 9999 →
    //   expect fill values skipped → nfuel_counts empty → NFUEL_CAT_BURNT not added
    // =========================================================
    @Test
    void testFindNfuelCatBurnt() throws Exception {
        // Fire > 0 in last time step with multiple fuel categories: Time=2, spatial=3x3
        // first time step (indices 0-8): all zeros; second time step (indices 9-11): fire > 0, fuel types {1,2,3}
        // expect: NFUEL_CAT_BURNT added with elementMap size=3, average = (1+2+3)/3 = 2.0
        var firePath = tempDir.resolve("nfuel_fire.nc").toString();
        NetcdfFileWriter w = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, firePath);
        try {
            Dimension time = w.addDimension(null, "Time", 2);
            Dimension r    = w.addDimension(null, "r", 3);
            Dimension c    = w.addDimension(null, "c", 3);
            w.addVariable(null, "FIRE_AREA", DataType.FLOAT, List.of(time, r, c));
            w.addVariable(null, "NFUEL_CAT",  DataType.FLOAT, List.of(time, r, c));
            w.create();
            float[] fireData = new float[18];
            fireData[9] = 1f; fireData[10] = 1f; fireData[11] = 1f;
            float[] nfuelData = new float[18];
            nfuelData[9] = 1f; nfuelData[10] = 2f; nfuelData[11] = 3f;
            w.write(w.findVariable("FIRE_AREA"), Array.factory(DataType.FLOAT, new int[]{2, 3, 3}, fireData));
            w.write(w.findVariable("NFUEL_CAT"),  Array.factory(DataType.FLOAT, new int[]{2, 3, 3}, nfuelData));
        } finally {
            w.close();
        }
        var fireReader = new NetcdfFileReader(firePath);
        var fireMeta = fireReader.processMetadata();
        fireReader.processFileContents(fireMeta, 1_000_000_000);
        var nfuelVar = fireMeta.variables.stream()
                .filter(v -> v.variableName.equals("NFUEL_CAT_BURNT")).findFirst().orElse(null);
        Assertions.assertNotNull(nfuelVar);
        Assertions.assertEquals(3, nfuelVar.elementMap.size());
        Assertions.assertEquals(2f, nfuelVar.average, 0.001f);

        // All fire_area = 0: no pixels burned → nfuel_counts empty → elementMap.size() == 0 → not added
        var zeroPath = tempDir.resolve("nfuel_zero.nc").toString();
        w = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, zeroPath);
        try {
            Dimension time = w.addDimension(null, "Time", 2);
            Dimension r    = w.addDimension(null, "r", 3);
            Dimension c    = w.addDimension(null, "c", 3);
            w.addVariable(null, "FIRE_AREA", DataType.FLOAT, List.of(time, r, c));
            w.addVariable(null, "NFUEL_CAT",  DataType.FLOAT, List.of(time, r, c));
            w.create();
            float[] zeros = new float[18];
            w.write(w.findVariable("FIRE_AREA"), Array.factory(DataType.FLOAT, new int[]{2, 3, 3}, zeros));
            w.write(w.findVariable("NFUEL_CAT"),  Array.factory(DataType.FLOAT, new int[]{2, 3, 3}, zeros));
        } finally {
            w.close();
        }
        var zeroMeta = new NetcdfFileReader(zeroPath).processFile(1_000_000_000);
        Assertions.assertTrue(zeroMeta.variables.stream()
                .noneMatch(v -> v.variableName.equals("NFUEL_CAT_BURNT")));

        // Fill value exclusion: FIRE_AREA has _FillValue=9999f, "fire" positions set to 9999
        // fill check skips those cells → no valid fire pixels → NFUEL_CAT_BURNT not added
        var fillPath = tempDir.resolve("nfuel_fill.nc").toString();
        w = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, fillPath);
        try {
            Dimension time = w.addDimension(null, "Time", 2);
            Dimension r    = w.addDimension(null, "r", 3);
            Dimension c    = w.addDimension(null, "c", 3);
            var fireVar = w.addVariable(null, "FIRE_AREA", DataType.FLOAT, List.of(time, r, c));
            fireVar.addAttribute(new Attribute("_FillValue", 9999f));
            w.addVariable(null, "NFUEL_CAT", DataType.FLOAT, List.of(time, r, c));
            w.create();
            float[] fillFireData = new float[18];
            fillFireData[9] = 9999f; fillFireData[10] = 9999f; // fill value, not real fire
            float[] nfuelData = new float[18];
            nfuelData[9] = 1f; nfuelData[10] = 2f;
            w.write(w.findVariable("FIRE_AREA"), Array.factory(DataType.FLOAT, new int[]{2, 3, 3}, fillFireData));
            w.write(w.findVariable("NFUEL_CAT"),  Array.factory(DataType.FLOAT, new int[]{2, 3, 3}, nfuelData));
        } finally {
            w.close();
        }
        var fillMeta = new NetcdfFileReader(fillPath).processFile(1_000_000_000);
        Assertions.assertTrue(fillMeta.variables.stream()
                .noneMatch(v -> v.variableName.equals("NFUEL_CAT_BURNT")));
    }

}