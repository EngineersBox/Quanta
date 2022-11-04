package com.engineersbox.quanta.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class FileUtils {

    private FileUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String readFile(final String path) {
        try {
            return org.apache.commons.io.FileUtils.readFileToString(
                    new File(Paths.get(path).toUri()),
                    StandardCharsets.UTF_8
            );
        } catch (final IOException e) {
            throw new RuntimeException(String.format(
                    "Unable to read file from path %s",
                    path
            ), e);
        }
    }

}
