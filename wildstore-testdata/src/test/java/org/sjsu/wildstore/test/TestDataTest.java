package org.sjsu.wildstore.test;

import edu.sjsu.wildstore.NetcdfFileReader;
import edu.sjsu.wildstore.TestDataDuplicator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

public class TestDataTest {
    @Test
    public void testTestData() throws URISyntaxException, IOException {
        var resourcesUrl = TestDataDuplicator.class.getResource("/testdata");
        var resourcesPath = Paths.get(resourcesUrl.toURI());
        var ncFiles = Files.walk(resourcesPath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".nc"))
                .toList();
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
