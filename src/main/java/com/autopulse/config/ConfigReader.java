package com.autopulse.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * ConfigReader - Singleton class that reads config.properties
 * and provides values to the entire framework.
 *
 * WHY SINGLETON?
 * Reading a file from disk is slow. We do it ONCE when the
 * framework starts, store the values in memory, and serve
 * from memory every time after that.
 */
public class ConfigReader {

    // The single instance — only one will ever exist
    private static ConfigReader instance;

    // Properties object holds all key=value pairs from the file
    private Properties properties;

    // Path to your config file
    private static final String CONFIG_PATH =
            "src/test/resources/config.properties";

    /**
     * PRIVATE constructor — this is the key to Singleton.
     * Private means nobody outside this class can say
     * "new ConfigReader()" — they MUST use getInstance().
     */
    private ConfigReader() {
        properties = new Properties();

        try {
            FileInputStream fileInput =
                    new FileInputStream(CONFIG_PATH);
            properties.load(fileInput);
            fileInput.close();
            System.out.println("✅ Config loaded successfully");

        } catch (IOException e) {
            System.out.println("❌ Config file not found at: "
                    + CONFIG_PATH);
            throw new RuntimeException(
                    "Cannot load config.properties", e);
        }
    }

    /**
     * getInstance() - The ONLY way to get a ConfigReader.
     *
     * First call  → creates the object, loads the file
     * Every call after → returns the SAME object, no file reading
     *
     * "synchronized" makes this thread-safe — safe when
     * multiple tests run in parallel simultaneously.
     */
    public static synchronized ConfigReader getInstance() {
        if (instance == null) {
            instance = new ConfigReader();
        }
        return instance;
    }

    /**
     * getProperty() - Get any value from config.properties
     * Example: getProperty("browser") returns "chrome"
     */
    public String getProperty(String key) {
        String value = properties.getProperty(key);

        if (value == null) {
            throw new RuntimeException(
                    "Property '" + key + "' not found in config.properties"
            );
        }
        return value.trim();
    }

    /**
     * Convenience methods — so callers don't need to
     * remember exact property key names.
     * Clean, readable, less error-prone.
     */
    public String getBaseUrl() {
        return getProperty("base.url");
    }

    public String getBrowser() {
        return getProperty("browser");
    }

    public int getImplicitWait() {
        return Integer.parseInt(getProperty("implicit.wait"));
    }

    public int getExplicitWait() {
        return Integer.parseInt(getProperty("explicit.wait"));
    }

    public String getApiBaseUrl() {
        return getProperty("api.base.url");
    }

    public boolean isAiEnabled() {
        return Boolean.parseBoolean(getProperty("ai.enabled"));
    }
}