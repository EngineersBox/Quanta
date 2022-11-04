package com.engineersbox.yajge.debug;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_NUM_EXTENSIONS;
import static org.lwjgl.opengl.GL30.glGetStringi;

public class GLVersion {

    private static GLVersion instance;

    public static int getMajorVersion() {
        return GLVersion.get().majorVersion;
    }

    public static int getMinorVersion() {
        return GLVersion.get().minorVersion;
    }

    public static double getVersion() {
        return GLVersion.get().version;
    }

    public static String getVersionString() {
        return GLVersion.get().versionString;
    }

    public static boolean isExtensionSupported(final String extension) {
        return GLVersion.get().extensionMap.containsKey(extension);
    }

    public static Collection<String> getSupportedExtensions() {
        return GLVersion.get().extensionMap.keySet();
    }

    private static GLVersion get() {
        if (GLVersion.instance == null) {
            GLVersion.instance = new GLVersion();
        }
        return GLVersion.instance;
    }

    private final String versionString;
    private final int majorVersion;
    private final int minorVersion;
    private final double version;
    private final Map<String, Boolean> extensionMap;

    private GLVersion() {
        this.versionString = glGetString(GL_VERSION);

        final int majorVersionIndex = this.versionString.indexOf('.');
        int minorVersionIndex = majorVersionIndex + 1;
        while (minorVersionIndex < this.versionString.length() && Character.isDigit(minorVersionIndex)) {
            minorVersionIndex++;
        }
        minorVersionIndex++;

        this.majorVersion = Integer.parseInt(this.versionString.substring(0, majorVersionIndex));
        this.minorVersion = Integer.parseInt(this.versionString.substring(majorVersionIndex + 1, minorVersionIndex));
        this.version = Double.parseDouble(this.versionString.substring(0, minorVersionIndex));

        final String[] supportedExtensions;
        if (this.majorVersion >= 3) {
            final int numExtensions = glGetInteger(GL_NUM_EXTENSIONS);
            supportedExtensions = new String[numExtensions];
            for (int i = 0; i < numExtensions; i++) {
                supportedExtensions[i] = glGetStringi(GL_EXTENSIONS, i);
            }
        } else {
            final String extensionsAsString = glGetString(GL_EXTENSIONS);
            supportedExtensions = extensionsAsString.split(" ");
        }

        this.extensionMap = new HashMap<>();
        for (final String extension : supportedExtensions) {
            this.extensionMap.put(extension, Boolean.TRUE);
        }
    }

}
