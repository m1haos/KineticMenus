package com.mdevstudio.kineticmenus.render;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

// Kept apart so PlaceholderAPI classes are only loaded when the plugin is installed.
final class PlaceholderBridge {

    private PlaceholderBridge() {
    }

    static String apply(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
