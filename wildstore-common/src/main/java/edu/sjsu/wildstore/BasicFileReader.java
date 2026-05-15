package edu.sjsu.wildstore;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class BasicFileReader implements FileReader {
    private final String filePath;

    public BasicFileReader(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void processFileContents(Metadata metadata, int maxReadSize) {}

    @Override
    public Metadata processMetadata() {
        DigestingRandomAccessFile randomAccessFile;
        try {
            randomAccessFile = new DigestingRandomAccessFile(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Metadata metadata = new Metadata();
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
            throw new RuntimeException(e);
        }

        return metadata;
    }
}
