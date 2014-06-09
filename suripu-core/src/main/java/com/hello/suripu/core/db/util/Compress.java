package com.hello.suripu.core.db.util;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by pangwu on 6/6/14.
 */
public class Compress {

    public static byte[] bzip2Compress(final byte[] uncompressed) throws IOException {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream(uncompressed.length);
        final BZip2CompressorOutputStream zipStream = new BZip2CompressorOutputStream(byteStream);
        IOException exceptionWhileCompressing = null;

        try {
            zipStream.write(uncompressed);
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


    public static byte[] bzip2Decompress(final byte[] compressed) throws IOException {

        final ByteArrayInputStream byteStream =
                new ByteArrayInputStream(compressed);
        final BZip2CompressorInputStream zipStream = new BZip2CompressorInputStream(byteStream);

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] byteBuffer = new byte[1024 * 8];

        IOException exceptionWhileDecompressing = null;

        try {
            int readCount = 0;

            while((readCount = zipStream.read(byteBuffer)) > 0){

                buffer.write(byteBuffer, 0, readCount);
            }

        } catch (IOException ex){
            exceptionWhileDecompressing = ex;
        }finally {
            buffer.close();
            zipStream.close();
            byteStream.close();

            if(exceptionWhileDecompressing != null){
                throw exceptionWhileDecompressing;
            }
        }

        return buffer.toByteArray();


    }

    public static byte[] gzipCompress(final byte[] uncompressed) throws IOException {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream(uncompressed.length);
        final GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
        IOException exceptionWhileCompressing = null;

        try {
            zipStream.write(uncompressed);
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

    public static byte[] gzipDecompress(final byte[] compressed) throws IOException {

        final ByteArrayInputStream byteStream =
                new ByteArrayInputStream(compressed);
        final GZIPInputStream zipStream = new GZIPInputStream(byteStream);

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] byteBuffer = new byte[1024 * 8];

        IOException exceptionWhileDecompressing = null;

        try {
            int readCount = 0;

            while((readCount = zipStream.read(byteBuffer)) > 0){

                buffer.write(byteBuffer, 0, readCount);
            }

        } catch (IOException ex){
            exceptionWhileDecompressing = ex;
        }finally {
            buffer.close();
            zipStream.close();
            byteStream.close();

            if(exceptionWhileDecompressing != null){
                throw exceptionWhileDecompressing;
            }
        }

        return buffer.toByteArray();


    }
}
