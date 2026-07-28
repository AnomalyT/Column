package com.anomalyt.column.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpServerBootstrap {
    public void start(int port) throws IOException {
        Path output = Paths.get("plugins", "column", "client-data.json");
        Files.writeString(output, "{}", StandardCharsets.UTF_8);
    }

    public void stop() {
    }
}
