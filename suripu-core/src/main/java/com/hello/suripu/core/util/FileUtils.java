package com.hello.suripu.core.util;

import com.google.common.io.Resources;

import java.io.File;

public class FileUtils {

    public static String getResourceFilePath(String resourceClassPathLocation) {
        try {
            return new File(Resources.getResource(resourceClassPathLocation)
                    .toURI()).getAbsolutePath();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
