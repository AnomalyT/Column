# Column build and run guide

## What this project is

Column is a client-owned Minecraft analytics stack:

- The Paper plugin records player movement, activity, and spawnpoint-like positions.
- The Fabric mod hosts a local heatmap dashboard and reads the plugin state from disk.
- The plugin stays silent in the server console.

## Requirements

### Windows
- JDK 21 or newer
- Gradle 9.x (or use the Gradle wrapper if you add one later)
- A Paper 1.21.11 server
- A Fabric 1.21.11 client install

### Linux
- JDK 21 or newer
- Gradle 9.x
- A Paper 1.21.11 server
- A Fabric 1.21.11 client install

## Build the project

From the repository root:

```bash
gradle :paper-plugin:jar :fabric-mod:jar
```

This produces:
- paper-plugin/build/libs/column-paper-plugin-0.1.0.jar
- fabric-mod/build/libs/column-fabric-mod-0.1.0.jar

## Windows instructions

1. Install JDK 21 and make sure java is on PATH.
2. Open PowerShell in the repository folder.
3. Run:

```powershell
gradle :paper-plugin:jar :fabric-mod:jar
```

4. Copy the Paper jar to your Paper server plugins folder.
5. Copy the Fabric jar to your Fabric client mods folder.
6. Start the server and join the world.
7. Open the local dashboard in your browser at:

```text
http://127.0.0.1:8765/
```

## Linux instructions

1. Install JDK 21 and Gradle.
2. Open a terminal in the repository folder.
3. Run:

```bash
gradle :paper-plugin:jar :fabric-mod:jar
```

4. Copy the Paper jar to your Paper server plugins folder.
5. Copy the Fabric jar to your Fabric client mods folder.
6. Start the server and join the world.
7. Open the local dashboard in your browser at:

```text
http://127.0.0.1:8765/
```

## Notes

- The plugin writes state to plugins/column/state.json.
- The Fabric mod reads that file and serves the dashboard locally.
- The UI is intentionally styled to feel close to a NOCOM-style monitoring dashboard while keeping the player, spawnpoint, and activity layers distinct.
