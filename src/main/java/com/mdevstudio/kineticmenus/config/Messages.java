package com.mdevstudio.kineticmenus.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Messages {

    public static final String RELOADED = "reloaded";
    public static final String PLAYERS_ONLY = "players-only";
    public static final String MENU_NOT_FOUND = "menu-not-found";
    public static final String LIST = "list";
    public static final String ANIMATIONS_ON = "animations-on";
    public static final String ANIMATIONS_OFF = "animations-off";

    static final Set<String> LANGUAGES = Set.of("ru", "en");

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final YamlConfiguration lines;
    private final TagResolver prefix;

    private Messages(YamlConfiguration lines) {
        this.lines = lines;
        this.prefix = Placeholder.parsed("prefix", line("prefix"));
    }

    public static Messages load(JavaPlugin plugin, String language) {
        for (String available : LANGUAGES) {
            String path = path(available);
            if (!new File(plugin.getDataFolder(), path).exists()) {
                plugin.saveResource(path, false);
            }
        }

        YamlConfiguration lines = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), path(language)));
        // Keys an admin deleted, or that appeared in an update, fall back to the bundled text.
        try (Reader bundled = new InputStreamReader(
                Objects.requireNonNull(plugin.getResource(path(language))), StandardCharsets.UTF_8)) {
            lines.setDefaults(YamlConfiguration.loadConfiguration(bundled));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Messages(lines);
    }

    public void send(CommandSender recipient, String key, TagResolver... placeholders) {
        recipient.sendMessage(component(key, placeholders));
    }

    public Component component(String key, TagResolver... placeholders) {
        return MINI_MESSAGE.deserialize(line(key), prefix, TagResolver.resolver(placeholders));
    }

    // getString(key, fallback) would skip the bundled defaults, so they are checked before falling back to the key.
    private String line(String key) {
        String line = lines.getString(key);
        return line != null ? line : key;
    }

    private static String path(String language) {
        return "lang/" + language + ".yml";
    }
}
