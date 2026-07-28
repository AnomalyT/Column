package com.anomalyt.column.plugin;

public record ActivityRecord(PlayerSnapshot snapshot, boolean active) {
    public String toJson() {
        return "{\"playerName\":\"" + snapshot.playerName() + "\",\"world\":\"" + snapshot.world() + "\",\"x\":" + snapshot.x() + ",\"y\":" + snapshot.y() + ",\"z\":" + snapshot.z() + ",\"active\":" + active + ",\"timestamp\":\"" + snapshot.timestamp() + "\"}";
    }
}
