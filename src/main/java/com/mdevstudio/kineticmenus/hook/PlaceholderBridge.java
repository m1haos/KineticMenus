package com.mdevstudio.kineticmenus.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

// Kept apart so PlaceholderAPI classes are only loaded when the plugin is installed.
public final class PlaceholderBridge {

    private PlaceholderBridge() {
    }

    public static String apply(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
