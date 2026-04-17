package edu.sjsu.wildstore;

public interface FileReader {

    /**
     * Processes common file metadata (filename, path, size, timestamps, digest).
     * All implementations must provide this method.
     */
    Metadata processMetadata();

    /**
     * Reads and processes file-type-specific contents into the provided metadata object.
     * Override to add content parsing for a specific file type.
     * By default, it does nothing.
     */
    void processFileContents(Metadata metadata, int maxReadSize);

    /**
     * Process the file that calls the metaData method, processMetadata and the read method, processFileContents
     * returns the resulting metadata.
     */
    default Metadata processFile(int maxReadSize) {
        Metadata metadata = processMetadata();
        processFileContents(metadata, maxReadSize);
        return metadata;
    }
}
