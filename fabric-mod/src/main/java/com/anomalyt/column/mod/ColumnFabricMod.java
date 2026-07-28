package com.anomalyt.column.mod;

import net.fabricmc.api.ClientModInitializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ColumnFabricMod implements ClientModInitializer {
    private static final int DEFAULT_DASHBOARD_PORT = 8765;
    private static volatile ColumnFabricMod instance;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ServerSocket serverSocket;
    private Thread serverThread;
    private int boundPort = -1;
    private volatile boolean overlayEnabled = true;
    private volatile String lastStatusLine = "Column idle";

    @Override
    public void onInitializeClient() {
        try {
            instance = this;
            ColumnStatusCommand.register();
            start();
        } catch (Throwable ignored) {
            // Keep Column from crashing the client if its local dashboard setup fails.
        }
    }

    public void start() {
        if (serverSocket != null || serverThread != null) {
            return;
        }

        for (int port : new int[]{DEFAULT_DASHBOARD_PORT, 8766, 0}) {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress("127.0.0.1", port));
                boundPort = serverSocket.getLocalPort();
                String readyMessage = "[Column] Dashboard ready at http://127.0.0.1:" + boundPort + "/ | status=" + buildDebugSummary();
                System.out.println(readyMessage);
                System.err.println(readyMessage);
                serverThread = new Thread(this::acceptLoop, "column-dashboard");
                serverThread.setDaemon(true);
                serverThread.start();
                lastStatusLine = "dashboard ready";
                scheduler.scheduleAtFixedRate(this::refresh, 0, 2, TimeUnit.SECONDS);
                break;
            } catch (Throwable error) {
                System.err.println("[Column] Dashboard bind failed on port " + port + ": " + error.getMessage());
            }
        }
    }

    public void stop() {
        try {
            scheduler.shutdownNow();
        } catch (Throwable ignored) {
        }
        try {
            if (serverThread != null) {
                serverThread.interrupt();
            }
        } catch (Throwable ignored) {
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Throwable ignored) {
        }
        serverSocket = null;
        serverThread = null;
        boundPort = -1;
    }

    public int getDashboardPort() {
        return boundPort;
    }

    public static ColumnFabricMod getInstance() {
        return instance;
    }

    public void setOverlayEnabled(boolean enabled) {
        this.overlayEnabled = enabled;
    }

    public String buildDebugSummary() {
        return "Column HUD " + (overlayEnabled ? "on" : "off") + " | dashboard=" + (boundPort > 0 ? "http://127.0.0.1:" + boundPort + "/" : "offline") + " | status=" + lastStatusLine;
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
                                heatmap.innerHTML = '<div style="position:absolute;inset:0;display:grid;place-items:center;color:#94a3b8;">No layers enabled</div>';
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
        try {
            Path dataFile = Paths.get("mods", "column", "state.json");
            Files.createDirectories(dataFile.getParent());
            Files.writeString(dataFile, "{\"status\":\"ready\"}", StandardCharsets.UTF_8);
            lastStatusLine = "state ready";
        } catch (Throwable ignored) {
            lastStatusLine = "state pending";
        }
        updateOverlayInGame();
    }

    private void acceptLoop() {
        while (!Thread.currentThread().isInterrupted() && serverSocket != null) {
            try (Socket client = serverSocket.accept()) {
                handleClient(client);
            } catch (SocketException ignored) {
                break;
            } catch (IOException ignored) {
            }
        }
    }

    private void handleClient(Socket client) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             OutputStream output = client.getOutputStream()) {
            String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }

            String[] requestParts = requestLine.split(" ");
            String path = requestParts.length > 1 ? requestParts[1] : "/";
            String[] pathParts = path.split("\\?", 2);
            String requestPath = pathParts[0];

            byte[] payload;
            String contentType;
            if ("/health".equals(requestPath)) {
                payload = "ok".getBytes(StandardCharsets.UTF_8);
                contentType = "text/plain; charset=utf-8";
            } else if ("/api/state".equals(requestPath) || "/players".equals(requestPath) || "/activity".equals(requestPath)) {
                payload = readStatePayload();
                contentType = "application/json; charset=utf-8";
            } else {
                payload = buildDashboardPage().getBytes(StandardCharsets.UTF_8);
                contentType = "text/html; charset=utf-8";
            }

            String response = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: " + contentType + "\r\n"
                    + "Content-Length: " + payload.length + "\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";
            output.write(response.getBytes(StandardCharsets.UTF_8));
            output.write(payload);
        }
    }

    private void updateOverlayInGame() {
        if (!overlayEnabled) {
            return;
        }

        try {
            Class<?> minecraftClientClass = Class.forName("net.minecraft.client.MinecraftClient");
            Object minecraftClient = minecraftClientClass.getMethod("getInstance").invoke(null);
            Object inGameHud = minecraftClient.getClass().getField("inGameHud").get(minecraftClient);
            Class<?> textClass = Class.forName("net.minecraft.text.Text");
            Object text = textClass.getMethod("literal", String.class).invoke(null, buildDebugSummary());
            Method method = inGameHud.getClass().getMethod("setOverlayMessage", textClass, boolean.class);
            method.invoke(inGameHud, text, false);
        } catch (Throwable ignored) {
        }
    }

    private byte[] readStatePayload() {
        Path[] candidatePaths = {
                Paths.get("mods", "column", "state.json"),
                Paths.get("plugins", "column", "state.json"),
                Paths.get("state.json")
        };

        for (Path statePath : candidatePaths) {
            if (Files.exists(statePath)) {
                try {
                    return Files.readString(statePath).getBytes(StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                }
            }
        }

        return "{\"players\":[],\"spawnPoints\":[],\"activity\":[]}".getBytes(StandardCharsets.UTF_8);
    }
}
