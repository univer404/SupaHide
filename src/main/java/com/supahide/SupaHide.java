package com.supahide;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class SupaHide extends JavaPlugin {
    private static final Pattern SAFE_PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private final Set<UUID> hiddenPlayers = new HashSet<>();
    private File dataFile;
    private YamlConfiguration data;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dataFile = new File(getDataFolder(), "data.yml");
        loadData();
        getServer().getPluginManager().registerEvents(new HideJoinListener(this), this);
        getLogger().info("SupaHide enabled.");
    }

    @Override
    public void onDisable() {
        saveData();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("hide")) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color(getConfig().getString("messages.players-only", "<red>Only players can use this command.</red>")));
            return true;
        }
        if (!player.hasPermission("hide.use")) {
            sendFeedback(player, getConfig().getString("messages.no-permission", "<red>You don't have permission to use this.</red>"));
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (hiddenPlayers.remove(uuid)) {
            runConsoleCommand("restore-tag", player);
            runConsoleCommand("restore-skin", player);
            sendFeedback(player, getConfig().getString("messages.unscrambled", "<#A8A8A8>You unscrambled your username."));
        } else {
            hiddenPlayers.add(uuid);
            runConsoleCommand("scramble-tag", player);
            runConsoleCommand("disguise-skin", player);
            sendFeedback(player, getConfig().getString("messages.scrambled", "<#A8A8A8>You scrambled your username."));
        }
        saveData();
        return true;
    }

    void handleJoin(Player player) {
        if (!hiddenPlayers.contains(player.getUniqueId())) {
            return;
        }
        if (!player.hasPermission("hide.use")) {
            hiddenPlayers.remove(player.getUniqueId());
            saveData();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) {
                    runConsoleCommand("restore-tag", player);
                    runConsoleCommand("restore-skin", player);
                }
            }, 2L);
            return;
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline() && hiddenPlayers.contains(player.getUniqueId())) {
                runConsoleCommand("disguise-skin", player);
            }
        }, 2L);
    }

    private void runConsoleCommand(String key, Player player) {
        String template = getConfig().getString("commands." + key, "");
        if (template == null || template.isBlank()) {
            getLogger().warning("No command configured for commands." + key);
            return;
        }
        String playerName = player.getName();
        if (!SAFE_PLAYER_NAME.matcher(playerName).matches()) {
            getLogger().warning("Skipped command because the player name was not safe.");
            return;
        }
        String command = template.replace("{player}", playerName).trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private void sendFeedback(Player player, String message) {
        player.sendMessage(color(message));
        player.sendActionBar(color(message));
    }

    private net.kyori.adventure.text.Component color(String text) {
        if (text == null) {
            return net.kyori.adventure.text.Component.empty();
        }
        // Support MiniMessage hex/RGB tags and legacy ampersand colors.
        if (text.contains("<#") || text.contains("<red>") || text.contains("<gray>") || text.contains("</")) {
            return miniMessage.deserialize(text);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    private void loadData() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder.");
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        hiddenPlayers.clear();
        for (String value : data.getStringList("hidden-players")) {
            try {
                hiddenPlayers.add(UUID.fromString(value));
            } catch (IllegalArgumentException exception) {
                getLogger().warning("Ignoring invalid UUID in data.yml: " + value);
            }
        }
    }

    private void saveData() {
        if (data == null || dataFile == null) {
            return;
        }
        data.set("hidden-players", hiddenPlayers.stream().map(UUID::toString).toList());
        try {
            data.save(dataFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save data.yml: " + exception.getMessage());
        }
    }
}