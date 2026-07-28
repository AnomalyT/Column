package com.anomalyt.column.mod;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ColumnFabricMod {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private HttpServer server;

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(8765), 0);
            server.createContext("/", this::handleRoot);
            server.createContext("/health", this::handleHealth);
            server.createContext("/api/state", this::handleState);
            server.createContext("/players", this::handlePlayers);
            server.createContext("/activity", this::handleActivity);
            server.setExecutor(null);
            server.start();
        } catch (IOException ignored) {
        }
        scheduler.scheduleAtFixedRate(this::refresh, 0, 2, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
        if (server != null) {
            server.stop(0);
        }
    }

    public String buildDashboardPage() {
        return """
                <!doctype html>
                <html lang=\"en\">
                <head>
                    <meta charset=\"utf-8\">
                    <title>Column Heatmap</title>
                    <style>
                        :root { color-scheme: dark; }
                        body { font-family: Inter, Arial, sans-serif; margin: 0; padding: 20px; background: #060b14; color: #f8fafc; }
                        .toolbar { display: flex; gap: 10px; flex-wrap: wrap; margin: 14px 0 16px; }
                        .chip { padding: 8px 12px; border-radius: 999px; background: #111827; border: 1px solid #334155; cursor: pointer; user-select: none; }
                        .chip.active { background: #2563eb; border-color: #60a5fa; box-shadow: 0 0 0 1px rgba(96,165,250,0.35); }
                        .heatmap { width: 100%; max-width: 960px; height: 560px; border: 1px solid rgba(148,163,184,0.25); border-radius: 16px; background: linear-gradient(135deg, #020617, #0f172a 70%, #111827); position: relative; overflow: hidden; box-shadow: inset 0 0 60px rgba(59,130,246,0.12); }
                        .heatmap::before { content: \"\"; position: absolute; inset: 0; background-image: linear-gradient(rgba(148,163,184,0.12) 1px, transparent 1px), linear-gradient(90deg, rgba(148,163,184,0.12) 1px, transparent 1px); background-size: 48px 48px; pointer-events: none; }
                        .layer { position: absolute; border-radius: 999px; box-shadow: 0 0 18px rgba(255,255,255,0.16); }
                        .legend { margin-top: 12px; color: #cbd5e1; font-size: 0.95rem; }
                        .summary { margin-top: 8px; color: #94a3b8; font-size: 0.9rem; }
                    </style>
                </head>
                <body>
                    <h1>Column Heatmap</h1>
                    <div class=\"toolbar\">
                        <button class=\"chip active\" data-layer=\"activity\">Activity</button>
                        <button class=\"chip active\" data-layer=\"online\">Online Players</button>
                        <button class=\"chip\" data-layer=\"offline\">Offline Players</button>
                        <button class=\"chip active\" data-layer=\"spawnpoints\">Spawnpoints</button>
                    </div>
                    <div class=\"heatmap\" id=\"heatmap\"></div>
                    <div class=\"legend\">A NOCOM-inspired overlay with activity trails, player presence, and spawnpoint markers.</div>
                    <div class=\"summary\" id=\"summary\">Awaiting data…</div>
                    <script>
                        const state = { players: [], spawnPoints: [], activity: [] };
                        const layerButtons = Array.from(document.querySelectorAll('.chip'));
                        const layerState = { activity: true, online: true, offline: false, spawnpoints: true };
                        const heatmap = document.getElementById('heatmap');
                        const summary = document.getElementById('summary');

                        layerButtons.forEach(button => {
                            button.addEventListener('click', () => {
                                const layer = button.dataset.layer;
                                layerState[layer] = !layerState[layer];
                                button.classList.toggle('active', layerState[layer]);
                                render();
                            });
                        });

                        async function loadState() {
                            try {
                                const response = await fetch('/api/state');
                                const data = await response.json();
                                state.players = Array.isArray(data.players) ? data.players : [];
                                state.spawnPoints = Array.isArray(data.spawnPoints) ? data.spawnPoints : [];
                                state.activity = Array.isArray(data.activity) ? data.activity : [];
                                summary.textContent = `Players: ${state.players.length} • Spawnpoints: ${state.spawnPoints.length} • Activity events: ${state.activity.length}`;
                            } catch (error) {
                                summary.textContent = 'Waiting for Column data…';
                            }
                            render();
                        }

                        function render() {
                            heatmap.innerHTML = '';
                            if (!layerState.activity && !layerState.online && !layerState.offline && !layerState.spawnpoints) {
                                heatmap.innerHTML = '<div style=\"position:absolute;inset:0;display:grid;place-items:center;color:#94a3b8;\">No layers enabled</div>';
                                return;
                            }

                            const addMarker = (left, top, size, color, label) => {
                                const node = document.createElement('div');
                                node.className = 'layer';
                                node.style.left = left + '%';
                                node.style.top = top + '%';
                                node.style.width = size + 'px';
                                node.style.height = size + 'px';
                                node.style.background = color;
                                node.title = label;
                                heatmap.appendChild(node);
                            };

                            if (layerState.activity) {
                                state.activity.forEach((item, index) => {
                                    const left = 20 + (index % 7) * 10 + ((item.x || 0) % 9) * 0.8;
                                    const top = 18 + (index % 6) * 10 + ((item.z || 0) % 7) * 1.1;
                                    const size = 18 + ((item.y || 0) % 10) * 3;
                                    addMarker(left, top, size, 'rgba(37, 99, 235, 0.8)', item.playerName || 'activity');
                                });
                            }

                            if (layerState.online) {
                                state.players.filter(player => player.active).forEach((player, index) => {
                                    const left = 20 + ((player.x || 0) % 11) * 6;
                                    const top = 16 + ((player.z || 0) % 9) * 7;
                                    addMarker(left, top, 26, 'rgba(52, 211, 153, 0.95)', player.playerName || 'online');
                                });
                            }

                            if (layerState.offline) {
                                state.players.filter(player => !player.active).forEach((player, index) => {
                                    const left = 18 + ((player.x || 0) % 10) * 6;
                                    const top = 18 + ((player.z || 0) % 8) * 8;
                                    addMarker(left, top, 20, 'rgba(148, 163, 184, 0.8)', player.playerName || 'offline');
                                });
                            }

                            if (layerState.spawnpoints) {
                                state.spawnPoints.forEach((point, index) => {
                                    const left = 12 + (index % 8) * 10 + ((point.x || 0) % 5) * 1.2;
                                    const top = 10 + (index % 7) * 10 + ((point.z || 0) % 6) * 1.2;
                                    const marker = document.createElement('div');
                                    marker.className = 'layer';
                                    marker.style.left = left + '%';
                                    marker.style.top = top + '%';
                                    marker.style.width = '16px';
                                    marker.style.height = '16px';
                                    marker.style.background = 'rgba(248, 113, 113, 0.95)';
                                    marker.style.borderRadius = '0';
                                    marker.style.transform = 'rotate(45deg)';
                                    marker.title = point.playerName || 'spawnpoint';
                                    heatmap.appendChild(marker);
                                });
                            }
                        }

                        loadState();
                        setInterval(loadState, 4000);
                    </script>
                </body>
                </html>
                """;
    }

    private void refresh() {
        Path dataFile = Paths.get("mods", "column", "state.json");
        try {
            Files.createDirectories(dataFile.getParent());
            Files.writeString(dataFile, "{\"status\":\"ready\"}", StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        byte[] payload = buildDashboardPage().getBytes(StandardCharsets.UTF_8);
        send(exchange, 200, payload, "text/html; charset=utf-8");
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        send(exchange, 200, "ok".getBytes(StandardCharsets.UTF_8), "text/plain; charset=utf-8");
    }

    private void handleState(HttpExchange exchange) throws IOException {
        Path statePath = Paths.get("plugins", "column", "state.json");
        if (Files.exists(statePath)) {
            byte[] payload = Files.readString(statePath).getBytes(StandardCharsets.UTF_8);
            send(exchange, 200, payload, "application/json; charset=utf-8");
        } else {
            send(exchange, 200, "{\"players\":[],\"spawnPoints\":[],\"activity\":[]}".getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8");
        }
    }

    private void handlePlayers(HttpExchange exchange) throws IOException {
        handleState(exchange);
    }

    private void handleActivity(HttpExchange exchange) throws IOException {
        handleState(exchange);
    }

    private void send(HttpExchange exchange, int status, byte[] payload, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }
}
