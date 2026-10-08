package com.mdevstudio.kineticmenus.render;

import com.mdevstudio.kineticmenus.hook.PlaceholderBridge;
import com.mdevstudio.kineticmenus.menu.Icon;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Turns icons into items for one viewer. {phase} moves animated gradients: it runs from -1 to 1 and wraps.
 */
public final class IconRenderer {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final boolean placeholders;

    public IconRenderer(boolean placeholders) {
        this.placeholders = placeholders;
    }

    public ItemStack render(Icon icon, Player viewer, double phase) {
        ItemStack item = ItemStack.of(icon.material(), icon.amount());
        item.editMeta(meta -> {
            if (icon.name() != null) {
                meta.displayName(text(icon.name(), viewer, phase));
            }
            if (!icon.lore().isEmpty()) {
                meta.lore(icon.lore().stream().map(line -> text(line, viewer, phase)).toList());
            }
            if (icon.glint()) {
                meta.setEnchantmentGlintOverride(true);
            }
            if (icon.model() != null) {
                meta.setItemModel(icon.model());
            }
            meta.setHideTooltip(icon.hideTooltip());
        });
        return item;
    }

    public Component text(String text, Player viewer, double phase) {
        String filled = fill(text.replace("{phase}", String.format(Locale.ROOT, "%.2f", phase)), viewer);
        return MINI_MESSAGE.deserialize(filled).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * Fills {player} and PlaceholderAPI placeholders, for commands and conditions.
     */
    public String fill(String text, Player viewer) {
        String filled = text.replace("{player}", viewer.getName());
        return placeholders && filled.indexOf('%') >= 0 ? PlaceholderBridge.apply(viewer, filled) : filled;
    }
}
