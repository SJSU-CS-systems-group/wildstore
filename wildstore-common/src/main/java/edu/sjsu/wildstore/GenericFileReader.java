package edu.sjsu.wildstore;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class GenericFileReader {
    private String filePath;
    private DigestingRandomAccessFile randomAccessFile;
    public DigestingRandomAccessFile getRandomAccessFile() {
        return randomAccessFile;
    }

    public GenericFileReader(String filePath) {this.filePath = filePath;}

    public Metadata processFile() {
        try {
            randomAccessFile = new DigestingRandomAccessFile(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Metadata metadata = new Metadata();
        // no global attributes to extract,
        // only records the filename, file size, last modified time, and a digest (checksum)
        metadata.globalAttributes = List.of();
        File file = new File(filePath);
        metadata.fileName = Set.of(filePath);
        metadata.filePath = Set.of(filePath.substring(0, filePath.lastIndexOf('/') + 1));
        metadata.fileSize = file.length();
        metadata.lastModified = file.lastModified();
        try {
            metadata.digestString = randomAccessFile.getDigestString(true);
        } catch (IOException e) {
            metadata.digestString = null;
            System.out.println("No digest was found for this file.");
            throw new RuntimeException(e);
        }

        return metadata;
    }
}
