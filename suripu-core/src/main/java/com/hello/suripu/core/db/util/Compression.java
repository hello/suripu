package com.hello.suripu.core.db.util;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by pangwu on 6/6/14.
 *
 * I still decide to keep all the throws in the functions of this class
 * because the caller can know precisely what is going wrong under the hood.
 *
 */
public class Compression {

    public static enum CompressionType{
        UNKNOWN(0),
        NONE(1),
        GZIP(2),
        BZIP2(3);

        private int value;
        private CompressionType(int value){
            this.value = value;
        }

        public static CompressionType fromInt(int value){
            switch (value){
                case 1:
                    return NONE;
                case 2:
                    return GZIP;
                case 3:
                    return BZIP2;
                default:
                    return UNKNOWN;
            }
        }

        public int getValue(){
            return this.value;
        }
    };


    public static byte[] compress(final byte[] rawData, final CompressionType type) throws IOException {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream(rawData.length);
        OutputStream zipStream = null;

        switch (type){
            case GZIP:
                zipStream = new GZIPOutputStream(byteStream);
                break;
            case BZIP2:
                zipStream = new BZip2CompressorOutputStream(byteStream);
                break;
            case NONE:
                byteStream.close();
                return Arrays.copyOf(rawData, rawData.length);
            default:
                zipStream = new GZIPOutputStream(byteStream);
                break;
        }

        IOException exceptionWhileCompressing = null;

        try {
            zipStream.write(rawData);
        } catch (IOException ex){
            exceptionWhileCompressing = ex;
        }finally {
            zipStream.close();
            byteStream.close();

            if(exceptionWhileCompressing != null){

                // We close the stream and throw the shit out.
                throw exceptionWhileCompressing;
            }
        }

        final byte[] compressed = byteStream.toByteArray();

        return compressed;
    }


    public static byte[] decompress(final byte[] compressed, final CompressionType type) throws IOException {

        final ByteArrayInputStream byteStream = new ByteArrayInputStream(compressed);
        InputStream unzipStream = null;

        switch (type){
            case NONE:
                byteStream.close();
                return Arrays.copyOf(compressed, compressed.length);
            case GZIP:
                unzipStream = new GZIPInputStream(byteStream);
                break;
            case BZIP2:
                unzipStream = new BZip2CompressorInputStream(byteStream);
                break;
            default:
                unzipStream = new GZIPInputStream(byteStream);
                break;
        }

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] byteBuffer = new byte[1024 * 8];

        IOException exceptionWhileDecompressing = null;

        try {
            int readCount = 0;
            while((readCount = unzipStream.read(byteBuffer)) > 0){

                buffer.write(byteBuffer, 0, readCount);
            }

        } catch (IOException ex){
            exceptionWhileDecompressing = ex;
        }finally {
            buffer.close();
            unzipStream.close();
            byteStream.close();

            if(exceptionWhileDecompressing != null){
                throw exceptionWhileDecompressing;
            }
        }

        return buffer.toByteArray();


    }


    public static byte[] bzip2Compress(final byte[] uncompressed) throws IOException {
        return compress(uncompressed, CompressionType.BZIP2);
    }


    public static byte[] bzip2Decompress(final byte[] compressed) throws IOException {
        return decompress(compressed, CompressionType.BZIP2);
    }

    public static byte[] gzipCompress(final byte[] uncompressed) throws IOException {
        return compress(uncompressed, CompressionType.GZIP);

    }

    public static byte[] gzipDecompress(final byte[] compressed) throws IOException {
        return decompress(compressed, CompressionType.GZIP);
    }
}
