package com.mdevstudio.kineticmenus.menu;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/**
 * What a button looks like. Texts are MiniMessage with {player}, {phase} and PlaceholderAPI placeholders.
 */
public record Icon(Material material, int amount, String name, List<String> lore, boolean glint,
                   NamespacedKey model, boolean hideTooltip) {

    /**
     * Whether the icon has to be drawn again while the menu is open: animated gradients or placeholders.
     */
    public boolean live() {
        return isLive(name) || lore.stream().anyMatch(Icon::isLive);
    }

    public Icon withGlint() {
        return new Icon(material, amount, name, lore, true, model, hideTooltip);
    }

    private static boolean isLive(String text) {
        return text != null && (text.contains("{phase}") || text.indexOf('%') >= 0);
    }
}
