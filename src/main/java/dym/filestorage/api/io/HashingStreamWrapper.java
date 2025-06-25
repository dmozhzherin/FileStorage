package dym.filestorage.api.io;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.util.Objects.requireNonNull;

/**
 * A wrapper for an InputStream that computes the SHA-256 hash and the size of the data read from it.
 * Can be used to integrate with existing InputStream-based APIs.
 */
public class HashingStreamWrapper extends InputStream {

    private static final String DEFAULT_HASH_ALGORITHM = "SHA-256";

    private final InputStream sourceStream;
    private final MessageDigest messageDigest;

    @Getter
    private long bytesRead;

    @SneakyThrows(NoSuchAlgorithmException.class)
    public HashingStreamWrapper(InputStream sourceStream) {
        requireNonNull(sourceStream, "The source InputStream cannot be null.");
        this.sourceStream = sourceStream;
        this.messageDigest = MessageDigest.getInstance(DEFAULT_HASH_ALGORITHM);
    }

    @Override
    public int read() throws IOException {
        int b = sourceStream.read();
        if (b != -1) {
            messageDigest.update((byte) b);
            bytesRead++;
        }
        return b;
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        int bytes = sourceStream.read(b, off, len);
        if (bytes != -1) {
            messageDigest.update(b, off, bytes);
            bytesRead += bytes;
        }
        return bytes;
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        return sourceStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return sourceStream.available();
    }

    @Override
    public void close() throws IOException {
        sourceStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        sourceStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        sourceStream.reset();
        messageDigest.reset();
        bytesRead = 0;
    }


    @Override
    public boolean markSupported() {
        return sourceStream.markSupported();
    }

    public byte[] getHash() {
        return messageDigest.digest();
    }

}