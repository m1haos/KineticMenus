package com.mdevstudio.kineticmenus.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public record PluginConfig(String language) {

    private static final int VERSION = 1;
    private static final String DEFAULT_LANGUAGE = "ru";

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
        return new PluginConfig(language);
    }
}
