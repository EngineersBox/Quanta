package com.engineersbox.quanta.resources.loader;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ResourceLoader {

    private static final Logger LOGGER = LogManager.getLogger(ResourceLoader.class);

    private ResourceLoader() {
        throw new IllegalStateException("Utility class");
    }

    private static String formatFilename(final String filename) {
        if (filename.startsWith("/")) {
            return filename;
        }
        return "/" + filename;
    }

    public static ByteBuffer loadResource(final String fileName) throws IOException {
        try (final InputStream inputStream = ResourceLoader.class.getResourceAsStream(formatFilename(fileName))) {
            if (inputStream == null) {
                throw new IOException(String.format(
                        "Could not find resource: %s",
                        fileName
                ));
            }
            final byte[] data = IOUtils.toByteArray(inputStream);
            final ByteBuffer buffer = MemoryUtil.memCalloc(data.length);
            buffer.put(data);
            buffer.flip();
            return buffer;
        } catch (final IOException e) {
            LOGGER.error("Could not open resource {} as a stream", fileName, e);
        }
        return null;
    }

}
