package com.anomalyt.column.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ColumnPaperPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private final ColumnPlugin tracker = new ColumnPlugin();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Column config resource missing; continuing with defaults: " + ex.getMessage());
        }

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("columnstatus") != null) {
            getCommand("columnstatus").setExecutor(this);
        }
        try {
            tracker.start();
        } catch (IOException ignored) {
        }
        scheduler.scheduleAtFixedRate(this::flushSnapshot, 0, 2, TimeUnit.SECONDS);
        getLogger().info("Column enabled");
    }

    @Override
    public void onDisable() {
        scheduler.shutdownNow();
        tracker.stop();
        getLogger().info("Column disabled");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        tracker.recordMovement(player.getName(), player.getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        tracker.recordMovement(player.getName(), player.getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        tracker.recordMovement(player.getName(), player.getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            player.sendMessage("§aColumn is running on this server.");
            return true;
        }
        sender.sendMessage("Column is running on this server.");
        return true;
    }

    private void flushSnapshot() {
        try {
            Path pluginDir = Paths.get("plugins", "column");
            Files.createDirectories(pluginDir);
            Files.writeString(pluginDir.resolve("state.json"), tracker.buildStateJson(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
