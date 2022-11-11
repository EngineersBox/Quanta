package com.engineersbox.quanta.resources.config;

import com.typesafe.config.ConfigFactory;

import java.io.File;

public abstract class ConfigHandler {

    private static final String CONFIG_FILE_PARAMETER = "quanta.config";
    public static final Config CONFIG;

    static {
        final File configFile = new File(System.getProperty(ConfigHandler.CONFIG_FILE_PARAMETER));
        final com.typesafe.config.Config typesafeConfig = ConfigFactory.parseFile(configFile).resolve();
        CONFIG = new Config(typesafeConfig);
    }

    private ConfigHandler() {
        throw new IllegalStateException("Utility class");
    }

}
