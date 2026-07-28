package com.anomalyt.column.plugin;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnPluginTest {
    @Test
    void startStopAndRecordMovementStaySilent() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            ColumnPlugin plugin = new ColumnPlugin();
            plugin.start();
            plugin.recordMovement("Alice", "world", 0.0, 64.0, 0.0, true);
            plugin.stop();
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(output.toString(StandardCharsets.UTF_8).isBlank());
    }
}
