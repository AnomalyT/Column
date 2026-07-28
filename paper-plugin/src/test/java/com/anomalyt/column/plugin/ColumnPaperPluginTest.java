package com.anomalyt.column.plugin;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ColumnPaperPluginTest {
    @Test
    void pluginBundleIncludesDefaultConfigResource() {
        InputStream resource = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(resource, "The plugin should ship a default config.yml resource");
    }
}
