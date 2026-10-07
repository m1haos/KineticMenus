package com.mdevstudio.kineticmenus.menu;

import com.mdevstudio.kineticmenus.animation.ClickEffect;
import com.mdevstudio.kineticmenus.animation.Grid;
import com.mdevstudio.kineticmenus.animation.OpenEffect;
import com.mdevstudio.kineticmenus.menu.Button.ClickKind;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;

/**
 * Reads every {@code menus/*.yml}. A mistake in one button is reported with the file and key and the rest
 * of the menu still loads.
 */
public final class MenuLoader {

    private static final Map<String, ClickKind> CLICK_KEYS = Map.of(
            "on-click", ClickKind.ANY,
            "on-left-click", ClickKind.LEFT,
            "on-right-click", ClickKind.RIGHT,
            "on-shift-left-click", ClickKind.SHIFT_LEFT,
            "on-shift-right-click", ClickKind.SHIFT_RIGHT);

    private final Logger logger;

    public MenuLoader(Logger logger) {
        this.logger = logger;
    }

    public Map<String, Menu> loadAll(File folder) {
        Map<String, Menu> menus = new LinkedHashMap<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        for (File file : files != null ? files : new File[0]) {
            String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            Menu menu = load(id, YamlConfiguration.loadConfiguration(file), "menus/" + file.getName());
            if (menu != null) {
                menus.put(id, menu);
            }
        }
        return menus;
    }

    Menu load(String id, ConfigurationSection yaml, String file) {
        int rows = yaml.getInt("rows", 3);
        if (rows < 1 || rows > 6) {
            logger.warn("{}: rows should be from 1 to 6, got {}. Using 3.", file, rows);
            rows = 3;
        }
        Grid grid = new Grid(rows);

        List<Button> buttons = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("buttons");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Button button = button(key, section.getConfigurationSection(key), grid, file + ", buttons." + key);
                if (button != null) {
                    buttons.add(button);
                }
            }
        }

        return new Menu(
                id,
                yaml.getString("title", id),
                grid,
                effect(OpenEffect.class, yaml.getString("open.effect", "none"), OpenEffect.NONE,
                        file + ", open.effect"),
                Math.clamp(yaml.getInt("open.ticks", 12), 1, 100),
                sound(yaml.getString("open.sound"), file + ", open.sound"),
                yaml.isConfigurationSection("filler") ? icon(yaml.getConfigurationSection("filler"), file + ", filler")
                        : null,
                yaml.isConfigurationSection("ripple") ? icon(yaml.getConfigurationSection("ripple"), file + ", ripple")
                        : new Icon(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1, null, List.of(), false, null, true),
                List.copyOf(buttons),
                Math.clamp(yaml.getInt("refresh", 2), 1, 200));
    }

    private Button button(String id, ConfigurationSection yaml, Grid grid, String where) {
        if (yaml == null) {
            logger.warn("{}: is not a section, skipped.", where);
            return null;
        }
        List<Integer> slots = slots(yaml, grid, where);
        if (slots.isEmpty()) {
            logger.warn("{}: has no slot inside the menu, skipped.", where);
            return null;
        }
        Icon icon = icon(yaml, where);

        Map<ClickKind, List<Action>> actions = new EnumMap<>(ClickKind.class);
        CLICK_KEYS.forEach((key, kind) -> {
            if (yaml.contains(key)) {
                actions.put(kind, actions(yaml.getStringList(key), where + "." + key));
            }
        });

        return new Button(
                id,
                slots,
                icon,
                yaml.isConfigurationSection("pressed")
                        ? icon(yaml.getConfigurationSection("pressed"), where + ".pressed")
                        : icon.withGlint(),
                effect(ClickEffect.class, yaml.getString("effect", "press"), ClickEffect.PRESS, where + ".effect"),
                actions,
                yaml.getString("permission"),
                actions(yaml.getStringList("on-deny"), where + ".on-deny"),
                effect(ClickEffect.class, yaml.getString("deny-effect", "shake"), ClickEffect.SHAKE,
                        where + ".deny-effect"));
    }

    private Icon icon(ConfigurationSection yaml, String where) {
        String materialName = yaml.getString("material", "stone");
        Material material = Material.matchMaterial(materialName);
        if (material == null || !material.isItem() || material.isAir()) {
            logger.warn("{}: material '{}' is not an item. Using barrier.", where, materialName);
            material = Material.BARRIER;
        }

        NamespacedKey model = null;
        String modelName = yaml.getString("model");
        if (modelName != null) {
            model = NamespacedKey.fromString(modelName);
            if (model == null) {
                logger.warn("{}: model '{}' should look like namespace:path.", where, modelName);
            }
        }

        return new Icon(
                material,
                Math.clamp(yaml.getInt("amount", 1), 1, material.getMaxStackSize()),
                yaml.getString("name"),
                yaml.getStringList("lore"),
                yaml.getBoolean("glint", false),
                model,
                yaml.getBoolean("hide-tooltip", false));
    }

    private List<Integer> slots(ConfigurationSection yaml, Grid grid, String where) {
        List<Object> raw = new ArrayList<>();
        if (yaml.isList("slots")) {
            raw.addAll(yaml.getList("slots", List.of()));
        } else if (yaml.contains("slots")) {
            raw.add(yaml.get("slots"));
        }
        if (yaml.contains("slot")) {
            raw.add(yaml.get("slot"));
        }

        List<Integer> slots = new ArrayList<>();
        for (Object value : raw) {
            String text = String.valueOf(value).trim();
            try {
                if (text.contains("-")) {
                    String[] range = text.split("-", 2);
                    int last = Integer.parseInt(range[1].trim());
                    for (int slot = Integer.parseInt(range[0].trim()); slot <= last; slot++) {
                        slots.add(slot);
                    }
                } else {
                    slots.add(Integer.parseInt(text));
                }
            } catch (NumberFormatException e) {
                logger.warn("{}: slot '{}' should be a number or a range like 10-16.", where, text);
            }
        }
        List<Integer> inside = slots.stream().filter(slot -> slot >= 0 && slot < grid.size()).distinct().toList();
        if (inside.size() < slots.size()) {
            logger.warn("{}: some slots are outside a {}-row menu (0-{}), they are ignored.", where, grid.rows(),
                    grid.size() - 1);
        }
        return inside;
    }

    private List<Action> actions(List<String> lines, String where) {
        List<Action> actions = new ArrayList<>();
        for (String line : lines) {
            Action action = action(line, where);
            if (action != null) {
                actions.add(action);
            }
        }
        return List.copyOf(actions);
    }

    private Action action(String line, String where) {
        int colon = line.indexOf(':');
        String type = (colon < 0 ? line : line.substring(0, colon)).trim().toLowerCase(Locale.ROOT);
        String argument = colon < 0 ? "" : line.substring(colon + 1).trim();
        String[] words = argument.split("\\s+");
        return switch (type) {
            case "message" -> new Action.Message(argument);
            case "console" -> new Action.ConsoleCommand(argument);
            case "player" -> new Action.PlayerCommand(argument);
            case "sound" -> {
                NamespacedKey key = sound(words[0], where);
                yield key != null ? new Action.Sound(key, number(words, 1, 1f), number(words, 2, 1f)) : null;
            }
            case "open" -> new Action.Open(words[0].toLowerCase(Locale.ROOT), words.length > 1
                    ? effect(OpenEffect.class, words[1], null, where) : null);
            case "close" -> new Action.Close();
            case "refresh" -> new Action.Refresh();
            default -> {
                logger.warn("{}: unknown action '{}'. Known: message, console, player, sound, open, close, refresh.",
                        where, type);
                yield null;
            }
        };
    }

    private NamespacedKey sound(String name, String where) {
        if (name == null || name.isBlank()) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(name.trim().toLowerCase(Locale.ROOT));
        if (key == null) {
            logger.warn("{}: sound '{}' should look like block.note_block.pling or minecraft:ui.button.click.", where,
                    name);
        }
        return key;
    }

    private <E extends Enum<E>> E effect(Class<E> type, String name, E fallback, String where) {
        try {
            return Enum.valueOf(type, name.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            logger.warn("{}: '{}' is not one of {}.", where, name,
                    List.of(type.getEnumConstants()).toString().toLowerCase(Locale.ROOT));
            return fallback;
        }
    }

    private static float number(String[] words, int index, float fallback) {
        try {
            return words.length > index ? Float.parseFloat(words[index]) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
