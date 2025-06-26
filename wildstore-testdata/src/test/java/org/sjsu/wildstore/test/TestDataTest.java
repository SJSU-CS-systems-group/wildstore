package org.sjsu.wildstore.test;

import edu.sjsu.wildstore.NetcdfFileReader;
import edu.sjsu.wildstore.TestDataUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class TestDataTest {
    @Test
    public void testTestData(@TempDir Path tmpDir) throws URISyntaxException, IOException {
        TestDataUtils.extractTestData(tmpDir);
        List<Path> ncFiles;
        try (var walkStream = Files.walk(tmpDir)){
            ncFiles = walkStream.filter(Files::isRegularFile).toList();
        }
        var digestStrings = new HashSet<String>();
        for (var ncFile : ncFiles) {
            var cdfFile = new NetcdfFileReader(ncFile.toString());
            var meta = cdfFile.processFile(1024*1024);
            digestStrings.add(meta.digestString);
        }

        // we don't check for uniqueness when we duplicate the files, so we allow up
        // to 25% duplication
        int delta = digestStrings.size() / 4;
        Assertions.assertEquals((double)ncFiles.size(), (double)digestStrings.size(), delta);
    }
}
