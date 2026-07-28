# Column

Column is a Minecraft ecosystem project split into two parts:

- Paper/Velocity plugin: logs movement and activity, exposes JSON endpoints for player data, and can be extended into a full server-side tracker.
- Fabric client mod: connects to the plugin over HTTP and can render a heatmap view of recent player activity.

## Current scaffold

- Server plugin scaffold in [paper-plugin](paper-plugin)
- Fabric client scaffold in [fabric-mod](fabric-mod)
- JSON endpoints:
  - /health
  - /players
  - /activity

## Build

Run:

```bash
gradle :paper-plugin:jar :fabric-mod:jar
```

## Next steps

1. Add a real Paper listener to capture movement and activity from players.
2. Add a real Fabric client entrypoint and UI overlay for the heatmap.
3. Optionally support Velocity by exposing the same HTTP endpoints from a proxy-side module.
