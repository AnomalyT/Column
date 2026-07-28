package com.anomalyt.column.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ColumnPlugin {
    private final Path dataDir;
    private final Path activityLog;
    private final Path stateFile;
    private final Map<String, PlayerSnapshot> players = new ConcurrentHashMap<>();
    private final Map<String, PlayerSnapshot> spawnPoints = new ConcurrentHashMap<>();
    private final List<ActivityRecord> records = new CopyOnWriteArrayList<>();

    public ColumnPlugin() {
        this.dataDir = Paths.get("plugins", "column");
        this.activityLog = dataDir.resolve("activity.jsonl");
        this.stateFile = dataDir.resolve("state.json");
    }

    public void start() throws IOException {
        Files.createDirectories(dataDir);
    }

    public void stop() {
    }

    public void recordMovement(String playerName, String world, double x, double y, double z, boolean active) {
        PlayerSnapshot snapshot = new PlayerSnapshot(playerName, world, x, y, z, active, Instant.now());
        players.put(playerName, snapshot);
        spawnPoints.computeIfAbsent(playerName, ignored -> snapshot);
        ActivityRecord record = new ActivityRecord(snapshot, active);
        records.add(record);
        appendRecord(record);
        writeStateFile();
    }

    public List<PlayerSnapshot> snapshotPlayers() {
        return new ArrayList<>(players.values());
    }

    public List<PlayerSnapshot> snapshotSpawnPoints() {
        return new ArrayList<>(spawnPoints.values());
    }

    public List<ActivityRecord> recentActivity() {
        return new ArrayList<>(records);
    }

    public String buildStateJson() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("players", snapshotPlayers());
        payload.put("spawnPoints", snapshotSpawnPoints());
        payload.put("activity", recentActivity());
        return payloadToJson(payload);
    }

    private void appendRecord(ActivityRecord record) {
        try {
            Files.writeString(
                    activityLog,
                    record.toJson() + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }

    private void writeStateFile() {
        try {
            Files.writeString(stateFile, buildStateJson(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private String payloadToJson(Map<String, Object> payload) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) {
                builder.append(",");
            }
            first = false;
            builder.append('"').append(entry.getKey()).append('"').append(':');
            Object value = entry.getValue();
            if (value instanceof List<?> list) {
                builder.append('[');
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        builder.append(',');
                    }
                    builder.append(objectToJson(list.get(i)));
                }
                builder.append(']');
            } else {
                builder.append(objectToJson(value));
            }
        }
        builder.append("}");
        return builder.toString();
    }

    private String objectToJson(Object value) {
        if (value instanceof PlayerSnapshot snapshot) {
            return snapshot.toJson();
        }
        if (value instanceof ActivityRecord record) {
            return record.toJson();
        }
        if (value instanceof String string) {
            return '"' + string.replace("\"", "\\\"") + '"';
        }
        if (value instanceof Boolean bool) {
            return bool.toString();
        }
        if (value instanceof Number number) {
            return number.toString();
        }
        return "null";
    }
}
