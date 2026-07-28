package com.anomalyt.column.plugin;

import java.time.Instant;

public record PlayerSnapshot(String playerName, String world, double x, double y, double z, boolean active, Instant timestamp) {
    public String toJson() {
        return "{\"playerName\":\"" + playerName + "\",\"world\":\"" + world + "\",\"x\":" + x + ",\"y\":" + y + ",\"z\":" + z + ",\"active\":" + active + ",\"timestamp\":\"" + timestamp + "\"}";
    }
}
