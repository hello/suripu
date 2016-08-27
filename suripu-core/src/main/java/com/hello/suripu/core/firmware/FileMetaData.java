package com.hello.suripu.core.firmware;

import com.google.common.base.MoreObjects;
import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;
import java.util.Objects;

public class FileMetaData {

    private final int filesize;
    private final byte[] sha1;

    private FileMetaData(byte[] sha1, int filesize) {
        this.sha1 = sha1;
        this.filesize = filesize;
    }

    public static FileMetaData create(byte[] sha1, int filesize) {
        return new FileMetaData(sha1, filesize);
    }

    public byte[] sha1() {
        return sha1;
    }

    public int filesize() {
        return filesize;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FileMetaData.class)
                .add("sha1", Hex.encodeHexString(sha1))
                .add("file_size", filesize)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(sha1), filesize);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FileMetaData)) {
            return false;
        }

        final FileMetaData other = (FileMetaData) obj;
        return Arrays.equals(sha1, other.sha1()) &&
                Objects.equals(filesize, other.filesize());
    }
}
