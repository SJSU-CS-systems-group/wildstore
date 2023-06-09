package com.sjsu.wildfirestorage;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * a subclass of RandomAccessFile that calculates the SHA512 digest on the fly.
 */
public class DigestingRandomAccessFile extends RandomAccessFile {
    private byte[] digestBytes = null;
    long digestPosition = 0; // the offset the the digest is waiting to fill
    private MessageDigest digest;

    {
        try {
            digest = MessageDigest.getInstance("SHA512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    public DigestingRandomAccessFile(String location) throws IOException {
        super(location, "r");
    }

    @Override
    public void close() throws IOException {
        getDigest(true);
        super.close();
    }

    /**
     * get the digest for the file.
     * @param force read the file to the end if necessary.
     * @return null if file hasn't been completely read.
     * @throws IOException
     */
    public byte[] getDigest(boolean force) throws IOException {
        if (digestBytes == null && force) {
            long fileLen = file.length();
            while (digestPosition < fileLen) {
                long remaining = fileLen - digestPosition;
                file.getChannel().transferTo(digestPosition, remaining, new WritableByteChannel() {
                    long pos = digestPosition;
                    @Override
                    public int write(ByteBuffer bb) throws IOException {
                        var len = bb.remaining();
                        updateDigestAtPosition(pos, bb);
                        pos += len;
                        return len;
                    }

                    @Override
                    public boolean isOpen() {
                        return true;
                    }

                    @Override
                    public void close() {
                    }
                });
            }
            digestBytes = digest.digest();
        }
        return digestBytes;
    }

    /**
     * get the string encoding of the digest of the file.
     * @param force read the file to the end if necessary.
     * @return null if the file hasn't been completely read. otherwise,
     *              it will be the base64 encoding of the first 12 bytes.
     */
    public String getDigestString(boolean force) throws IOException {
        var digestBytes = getDigest(force);
        return digestBytes == null ? null :
            // since every 3 bytes expand to 4, we want chars 0 to 15
            Base64.getUrlEncoder().encodeToString(digestBytes).substring(0, 16);
    }

    protected boolean extendedModeWasSet;
    @Override
    public void setExtendMode() {
        extendedModeWasSet = true;
        super.setExtendMode();
    }

    @Override
    public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
        if (offset + nbytes > digestPosition) {
            dest = new DigestingWritableByteChannel(dest, offset);
        }
        return super.readToByteChannel(dest, offset, nbytes);
    }

    class DigestingWritableByteChannel implements WritableByteChannel {
        WritableByteChannel wrapped;
        long pos;

        DigestingWritableByteChannel(WritableByteChannel wrapped, long startingPos) {
            this.wrapped = wrapped;
            this.pos = startingPos;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            updateDigestAtPosition(pos, src);
            digest.update(src.duplicate());
            pos += src.remaining();
            return wrapped.write(src);
        }

        @Override
        public boolean isOpen() {
            return wrapped.isOpen();
        }

        @Override
        public void close() throws IOException {
            wrapped.close();
        }
    }

    @Override
    protected int read_(long pos, byte[] b, int offset, int len) throws IOException {
        var rc = super.read_(pos, b, offset, len);
        updateDigestAtPosition(pos, ByteBuffer.wrap(b, offset, rc));
        return rc;
    }

    private void updateDigestAtPosition(long pos, ByteBuffer bb) throws IOException {
        digestToPosition(pos);
        int trueLen = bb.remaining();
        if (digestPosition < pos + trueLen) {
            // number of bytes that digest is ahead of the read position.
            // we cannot be behind the read position because we called digestToPosition()
            // earlier.
            // this must be castable to in because it can differ by at most trueLen
            int bytesAhead = (int) (digestPosition - pos);
            // skip the bytes we've already read
            bb.position(bb.position() + bytesAhead);
            digestPosition += bb.remaining();
            digest.update(bb);
        }
    }

    private void digestToPosition(long pos) throws IOException {
        long oldPos = file.getFilePointer();
        file.seek(digestPosition);
        while (pos > digestPosition) {
            long count = pos - digestPosition;
            int toRead = count < 1024*1024 ? (int)count : 1024*1024;
            byte[] missingBytes = new byte[toRead];
            file.readFully(missingBytes);
            digest.update(missingBytes);
            digestPosition += toRead;
        }
        file.seek(oldPos);
    }
}
