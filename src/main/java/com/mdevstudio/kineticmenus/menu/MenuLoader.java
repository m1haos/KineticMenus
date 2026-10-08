package com.mdevstudio.kineticmenus.menu;

import com.mdevstudio.kineticmenus.animation.AmbientEffect;
import com.mdevstudio.kineticmenus.animation.ClickEffect;
import com.mdevstudio.kineticmenus.animation.Easing;
import com.mdevstudio.kineticmenus.animation.Grid;
import com.mdevstudio.kineticmenus.animation.OpenEffect;
import com.mdevstudio.kineticmenus.menu.Button.ClickKind;
import com.mdevstudio.kineticmenus.menu.Button.Reaction;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;

/**
 * Reads every {@code .yml} under the menus folder, subfolders included; the file name is the menu id.
 * A mistake in one button is reported with the file and key and the rest of the menu still loads.
 */
public final class MenuLoader {

    private static final Map<String, ClickKind> CLICK_KEYS = Map.of(
            "on-click", ClickKind.ANY,
            "on-left-click", ClickKind.LEFT,
            "on-right-click", ClickKind.RIGHT,
            "on-shift-left-click", ClickKind.SHIFT_LEFT,
            "on-shift-right-click", ClickKind.SHIFT_RIGHT);
    private static final Icon DEFAULT_ACCENT =
            new Icon(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1, null, List.of(), false, null, true);
    private static final Icon DEFAULT_TRAIL =
            new Icon(Material.GRAY_STAINED_GLASS_PANE, 1, null, List.of(), false, null, true);

    private final Logger logger;
    private final ScriptParser parser;

    public MenuLoader(Logger logger) {
        this.logger = logger;
        this.parser = new ScriptParser(logger);
    }

    public Map<String, Menu> loadAll(Path folder) {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(folder)) {
            files = walk.filter(path -> path.toString().endsWith(".yml")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Map<String, Menu> menus = new LinkedHashMap<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            String id = name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT);
            String shown = "menus/" + folder.relativize(file).toString().replace('\\', '/');
            if (menus.containsKey(id)) {
                logger.warn("{}: another file already defines menu '{}', this one is skipped.", shown, id);
                continue;
            }
            menus.put(id, load(id, YamlConfiguration.loadConfiguration(file.toFile()), shown));
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

        List<Condition> openIf = new ArrayList<>(parser.conditions(yaml.getStringList("open-if"), file + ", open-if"));
        if (yaml.isString("permission")) {
            openIf.addFirst(new Condition.Permission(yaml.getString("permission")));
        }

        return new Menu(
                id,
                yaml.getString("title", id),
                grid,
                commands(yaml),
                List.copyOf(openIf),
                parser.actions(yaml.getStringList("on-deny"), file + ", on-deny"),
                opening(yaml, file),
                new Menu.Ambient(
                        parser.constant(AmbientEffect.class, yaml.getString("ambient.effect", "none"),
                                AmbientEffect.NONE, file + ", ambient.effect"),
                        Math.clamp(yaml.getInt("ambient.speed", 2), 1, 40)),
                optionalIcon(yaml, "filler", file, null),
                optionalIcon(yaml, "accent", file, DEFAULT_ACCENT),
                optionalIcon(yaml, "trail", file, DEFAULT_TRAIL),
                List.copyOf(buttons),
                Math.clamp(yaml.getInt("refresh", 2), 1, 200));
    }

    private Menu.Opening opening(ConfigurationSection yaml, String file) {
        String easing = yaml.getString("open.easing", ScriptParser.AUTO);
        return new Menu.Opening(
                parser.constant(OpenEffect.class, yaml.getString("open.effect", "none"), OpenEffect.NONE,
                        file + ", open.effect"),
                Math.clamp(yaml.getInt("open.ticks", 16), 1, 100),
                easing.equalsIgnoreCase(ScriptParser.AUTO) ? null
                        : parser.constant(Easing.class, easing, null, file + ", open.easing"),
                parser.sounds(yaml.get("open.sound"), file + ", open.sound"));
    }

    private static List<String> commands(ConfigurationSection yaml) {
        List<String> commands = new ArrayList<>(yaml.getStringList("commands"));
        if (yaml.isString("command")) {
            commands.addFirst(yaml.getString("command"));
        }
        return commands.stream()
                .map(command -> command.trim().toLowerCase(Locale.ROOT))
                .map(command -> command.startsWith("/") ? command.substring(1) : command)
                .filter(command -> !command.isEmpty())
                .distinct()
                .toList();
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
                actions.put(kind, parser.actions(yaml.getStringList(key), where + "." + key));
            }
        });

        List<Condition> clickIf = new ArrayList<>(parser.conditions(yaml.getStringList("click-if"),
                where + ".click-if"));
        if (yaml.isString("permission")) {
            clickIf.addFirst(new Condition.Permission(yaml.getString("permission")));
        }

        return new Button(
                id,
                slots,
                yaml.getInt("priority", 0),
                icon,
                yaml.isConfigurationSection("pressed")
                        ? icon(yaml.getConfigurationSection("pressed"), where + ".pressed")
                        : icon.withGlint(),
                parser.conditions(yaml.getStringList("show-if"), where + ".show-if"),
                List.copyOf(clickIf),
                parser.constant(ClickEffect.class, yaml.getString("effect", "press"), ClickEffect.PRESS,
                        where + ".effect"),
                parser.sounds(yaml.get("sound"), where + ".sound"),
                actions,
                new Reaction(
                        parser.constant(ClickEffect.class, yaml.getString("deny-effect", "shake"), ClickEffect.SHAKE,
                                where + ".deny-effect"),
                        parser.sounds(yaml.get("deny-sound"), where + ".deny-sound"),
                        parser.actions(yaml.getStringList("on-deny"), where + ".on-deny")));
    }

    private Icon optionalIcon(ConfigurationSection yaml, String key, String file, Icon fallback) {
        return yaml.isConfigurationSection(key) ? icon(yaml.getConfigurationSection(key), file + ", " + key)
                : fallback;
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
        if (slots.stream().anyMatch(slot -> slot < 0 || slot >= grid.size())) {
            logger.warn("{}: some slots are outside a {}-row menu (0-{}), they are ignored.", where, grid.rows(),
                    grid.size() - 1);
        }
        return inside;
    }
}
