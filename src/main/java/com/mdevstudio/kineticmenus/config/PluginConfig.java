package com.mdevstudio.kineticmenus.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public record PluginConfig(String language, boolean autoReload, boolean sounds, float soundVolume,
                           boolean bedrockReducedMotion) {

    private static final int VERSION = 2;
    private static final String DEFAULT_LANGUAGE = "ru";
    private static final float MAX_VOLUME = 2;

    public static PluginConfig load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration yaml = plugin.getConfig();

        if (yaml.getInt("config-version") < VERSION) {
            // Admins keep their values; only keys added in newer versions are written in.
            yaml.options().copyDefaults(true);
            yaml.set("config-version", VERSION);
            plugin.saveConfig();
        }

        String language = yaml.getString("language", DEFAULT_LANGUAGE);
        if (!Messages.LANGUAGES.contains(language)) {
            plugin.getSLF4JLogger().warn("config.yml: language '{}' is not available, expected one of {}. Using '{}'.",
                    language, Messages.LANGUAGES, DEFAULT_LANGUAGE);
            language = DEFAULT_LANGUAGE;
        }

        double volume = yaml.getDouble("sounds.volume", 1);
        if (volume < 0 || volume > MAX_VOLUME) {
            plugin.getSLF4JLogger().warn("config.yml: sounds.volume should be from 0 to {}, got {}. Using 1.",
                    MAX_VOLUME, volume);
            volume = 1;
        }

        return new PluginConfig(
                language,
                yaml.getBoolean("auto-reload", true),
                yaml.getBoolean("sounds.enabled", true),
                (float) volume,
                yaml.getBoolean("bedrock-reduced-motion", true));
    }
}
