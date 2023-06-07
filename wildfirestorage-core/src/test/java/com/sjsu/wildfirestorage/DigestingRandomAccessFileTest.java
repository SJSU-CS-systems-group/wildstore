package com.sjsu.wildfirestorage;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class DigestingRandomAccessFileTest extends TestCase {
    public void testRandomAccessFile() throws NoSuchAlgorithmException, IOException {
        var dis = new DigestInputStream(DigestingRandomAccessFileTest.class.getResourceAsStream("/wrfout.nc"),
                MessageDigest.getInstance("SHA512"));
        try (var fis = new FileOutputStream("test.nc")) {
            dis.transferTo(fis);
        }
        var readDigest = dis.getMessageDigest().digest();
        var reader = new NetcdfFileReader("test.nc");
        reader.processFile();
        var readerDigest = reader.getRandomAccessFile().getDigest(true);
        assertTrue("file digest and DigestingRandomAccessFile do not match", Arrays.equals(readerDigest, readDigest));
        new File("test.nc").delete();
        assertNotNull(reader.getRandomAccessFile().getDigestString(false));
    }
}
