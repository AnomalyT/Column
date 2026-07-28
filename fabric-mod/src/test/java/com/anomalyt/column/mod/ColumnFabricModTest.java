package com.anomalyt.column.mod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnFabricModTest {
    @Test
    void buildDashboardPageContainsHeatmapMarkup() {
        ColumnFabricMod mod = new ColumnFabricMod();
        String page = mod.buildDashboardPage();

        assertTrue(page.contains("Column Heatmap"));
        assertTrue(page.contains("Activity"));
        assertTrue(page.contains("Online Players"));
        assertTrue(page.contains("Offline Players"));
        assertTrue(page.contains("Spawnpoints"));
        assertTrue(page.contains("/api/state"));
        assertTrue(page.contains("heatmap"));
    }
}
