import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import edu.sjsu.wildstore.NetcdfFileReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DigestingRandomAccessFileTest {
    @Test
    public void testRandomAccessFile() throws NoSuchAlgorithmException, IOException {
        var dis = new DigestInputStream(DigestingRandomAccessFileTest.class.getResourceAsStream("/wrfout.nc"),
                MessageDigest.getInstance("SHA512"));
        try (var fis = new FileOutputStream("test.nc")) {
            dis.transferTo(fis);
        }
        var readDigest = dis.getMessageDigest().digest();
        var reader = new NetcdfFileReader("test.nc");
        int maxReadSize = 1000000000;

        reader.processFile(maxReadSize);
        var readerDigest = reader.getRandomAccessFile().getDigest(true);
        Assertions.assertArrayEquals(readerDigest, readDigest);
        new File("test.nc").delete();
        Assertions.assertNotNull(reader.getRandomAccessFile().getDigestString(false));
    }
}
