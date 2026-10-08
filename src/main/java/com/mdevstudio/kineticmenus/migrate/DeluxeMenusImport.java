package com.mdevstudio.kineticmenus.migrate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;

/**
 * Turns DeluxeMenus menus into KineticMenus files. Everything with a counterpart is carried over; anything
 * else is left out with a warning naming the file and key, so the admin knows what to look at.
 */
public final class DeluxeMenusImport {

    /**
     * @param imported ids of the menus written
     * @param skipped ids that already had a file and were left alone
     */
    public record Report(List<String> imported, List<String> skipped, int warnings) {
    }

    private static final Map<String, String> CLICK_KEYS = Map.of(
            "click_commands", "on-click",
            "left_click_commands", "on-left-click",
            "right_click_commands", "on-right-click",
            "shift_left_click_commands", "on-shift-left-click",
            "shift_right_click_commands", "on-shift-right-click");
    private static final List<String> CLICK_REQUIREMENTS = List.of("click_requirement", "left_click_requirement",
            "right_click_requirement", "shift_left_click_requirement", "shift_right_click_requirement");
    private static final List<String> COMPARISONS = List.of("==", "!=", ">=", "<=", ">", "<");
    private static final List<String> HEAD_PREFIXES = List.of("head-", "basehead-", "texture-", "hdb-",
            "placeholder-", "itemsadder-", "oraxen-", "nexo-", "mmoitems-", "executableitems-");
    private static final Pattern ACTION = Pattern.compile("^\\[([a-zA-Z_]+)]\\s*(.*)$");
    private static final Pattern MODIFIER = Pattern.compile("<(delay|chance)=[^>]*>");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.AMPERSAND_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final Logger logger;
    private final Function<String, String> soundKeys;
    private int warnings;

    /**
     * @param soundKeys turns a Bukkit sound name like {@code ENTITY_PLAYER_LEVELUP} into its key, or {@code null}
     */
    public DeluxeMenusImport(Logger logger, Function<String, String> soundKeys) {
        this.logger = logger;
        this.soundKeys = soundKeys;
    }

    /**
     * @param deluxeMenus the {@code plugins/DeluxeMenus} folder
     * @param target where the new menu files go
     */
    public Report run(Path deluxeMenus, Path target) {
        Map<String, Source> sources = sources(deluxeMenus);
        List<String> imported = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        try {
            Files.createDirectories(target);
            for (Map.Entry<String, Source> entry : sources.entrySet()) {
                String id = entry.getKey();
                Path file = target.resolve(id + ".yml");
                if (Files.exists(file)) {
                    skipped.add(id);
                    continue;
                }
                YamlConfiguration menu = convert(entry.getValue().menu(), entry.getValue().name());
                menu.options().setHeader(List.of("Imported from DeluxeMenus/" + entry.getValue().name() + "."));
                menu.save(file.toFile());
                imported.add(id);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Report(imported, skipped, warnings);
    }

    YamlConfiguration convert(ConfigurationSection menu, String where) {
        YamlConfiguration out = new YamlConfiguration();
        out.set("title", text(menu.getString("menu_title", "")));
        String type = menu.getString("inventory_type", "CHEST");
        if (!type.equalsIgnoreCase("chest")) {
            warn(where, "inventory_type", type + " menus are not supported, made a chest menu instead");
        }
        out.set("rows", Math.clamp((menu.getInt("size", 54) + 8) / 9, 1, 6));

        List<String> commands = menu.isList("open_command") ? menu.getStringList("open_command")
                : menu.isString("open_command") ? List.of(menu.getString("open_command")) : List.of();
        if (!commands.isEmpty()) {
            out.set("commands", commands);
        }
        Requirement open = requirement(menu.getConfigurationSection("open_requirement"), where + ", open_requirement");
        if (!open.conditions().isEmpty()) {
            out.set("open-if", open.conditions());
        }
        if (!open.deny().isEmpty()) {
            out.set("on-deny", open.deny());
        }
        out.set("open.effect", "cascade");
        if (menu.contains("update_interval")) {
            out.set("refresh", Math.clamp(menu.getInt("update_interval") * 20L, 1, 200));
        }

        ConfigurationSection items = menu.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection item = items.getConfigurationSection(key);
                if (item != null) {
                    Map<String, Object> button = button(item, where + ", items." + key);
                    if (button != null) {
                        out.createSection("buttons." + key.replace('.', '_'), button);
                    }
                }
            }
        }
        return out;
    }

    private Map<String, Object> button(ConfigurationSection item, String where) {
        Map<String, Object> button = new LinkedHashMap<>();
        String material = item.getString("material", "stone").trim();
        String lower = material.toLowerCase(Locale.ROOT);
        if (lower.equals("air")) {
            warn(where, "material", "air buttons are invisible and cannot be shown, skipped");
            return null;
        }
        if (HEAD_PREFIXES.stream().anyMatch(lower::startsWith)) {
            warn(where, "material", "'" + material + "' needs a plugin or a texture, used a player head");
            lower = "player_head";
        }
        button.put("material", lower);

        copy(item, "slot", button, "slot");
        copy(item, "slots", button, "slots");
        copy(item, "priority", button, "priority");
        copy(item, "amount", button, "amount");
        copy(item, "item_model", button, "model");
        copy(item, "hide_tooltip", button, "hide-tooltip");
        if (item.contains("model_data")) {
            warn(where, "model_data", "custom model data is not supported, use item_model resource pack models");
        }
        if (item.isString("display_name")) {
            button.put("name", text(item.getString("display_name")));
        }
        if (item.isList("lore")) {
            button.put("lore", item.getStringList("lore").stream().map(this::text).toList());
        }
        if (!item.getStringList("enchantments").isEmpty() || item.getBoolean("enchantment_glint_override")) {
            button.put("glint", true);
        }

        Requirement view = requirement(item.getConfigurationSection("view_requirement"), where + ".view_requirement");
        if (!view.conditions().isEmpty()) {
            button.put("show-if", view.conditions());
        }

        List<String> requirementKeys = CLICK_REQUIREMENTS.stream().filter(item::isConfigurationSection).toList();
        if (!requirementKeys.isEmpty()) {
            if (requirementKeys.size() > 1) {
                warn(where, requirementKeys.get(1), "only one click requirement is supported, used "
                        + requirementKeys.getFirst() + " for every click");
            }
            String key = requirementKeys.getFirst();
            Requirement click = requirement(item.getConfigurationSection(key), where + "." + key);
            button.put("click-if", click.conditions());
            if (!click.deny().isEmpty()) {
                button.put("on-deny", click.deny());
            }
        }

        CLICK_KEYS.forEach((from, to) -> {
            if (item.isList(from)) {
                button.put(to, actions(item.getStringList(from), where + "." + from));
            }
        });
        if (item.contains("middle_click_commands")) {
            warn(where, "middle_click_commands", "middle clicks are not supported, left out");
        }
        return button;
    }

    private Requirement requirement(ConfigurationSection section, String where) {
        if (section == null) {
            return new Requirement(List.of(), List.of());
        }
        if (section.contains("minimum_requirements")) {
            warn(where, "minimum_requirements", "every requirement is now needed, not just the minimum");
        }
        List<String> conditions = new ArrayList<>();
        List<String> deny = new ArrayList<>(actions(section.getStringList("deny_commands"), where + ".deny_commands"));
        ConfigurationSection requirements = section.getConfigurationSection("requirements");
        if (requirements != null) {
            for (String key : requirements.getKeys(false)) {
                ConfigurationSection requirement = requirements.getConfigurationSection(key);
                if (requirement != null) {
                    String here = where + ".requirements." + key;
                    conditions.addAll(conditions(requirement, here));
                    deny.addAll(actions(requirement.getStringList("deny_commands"), here + ".deny_commands"));
                }
            }
        }
        return new Requirement(conditions, deny);
    }

    private List<String> conditions(ConfigurationSection requirement, String where) {
        String type = requirement.getString("type", "").trim().toLowerCase(Locale.ROOT);
        String input = requirement.getString("input", "");
        String output = requirement.getString("output", "");
        if (COMPARISONS.contains(type)) {
            return List.of("placeholder: " + input + " " + type + " " + output);
        }
        boolean negated = type.startsWith("!");
        String bare = negated ? type.substring(1).trim() : type;
        String not = negated ? "!" : "";
        return switch (bare) {
            case "has permission" -> List.of(not + "permission: " + requirement.getString("permission", ""));
            case "has permissions" -> {
                if (requirement.contains("minimum")) {
                    warn(where, "minimum", "every permission in the list is now needed");
                }
                yield requirement.getStringList("permissions").stream().map(node -> not + "permission: " + node)
                        .toList();
            }
            case "has money" -> {
                String amount = requirement.getString("amount", "0");
                if (amount.matches("\\d+(\\.\\d+)?")) {
                    yield List.of(not + "money: " + amount);
                }
                yield List.of(not + "placeholder: %vault_eco_balance% >= " + amount);
            }
            case "string equals", "string equals ignorecase" -> {
                if (bare.endsWith("ignorecase")) {
                    warn(where, "type", "comparison is now case sensitive");
                }
                yield List.of("placeholder: " + input + (negated ? " != " : " == ") + output);
            }
            default -> {
                warn(where, "type", "requirement '" + type + "' has no counterpart, left out");
                yield List.of();
            }
        };
    }

    private List<String> actions(List<String> lines, String where) {
        List<String> actions = new ArrayList<>();
        for (String line : lines) {
            String action = action(line, where);
            if (action != null) {
                actions.add(action);
            }
        }
        return actions;
    }

    private String action(String line, String where) {
        String clean = MODIFIER.matcher(line).replaceAll("").trim();
        if (!clean.equals(line.trim())) {
            warn(where, line, "delay and chance are not supported, the action runs at once");
        }
        Matcher matcher = ACTION.matcher(clean);
        if (!matcher.matches()) {
            warn(where, line, "not an action, left out");
            return null;
        }
        String argument = matcher.group(2).trim();
        return switch (matcher.group(1).toLowerCase(Locale.ROOT)) {
            case "player", "commandevent" -> "player: " + argument;
            case "console" -> "console: " + argument;
            case "message" -> "message: " + text(argument);
            case "minimessage" -> "message: " + argument;
            case "openguimenu", "openmenu" -> "open: " + argument.toLowerCase(Locale.ROOT);
            case "close" -> "close";
            case "refresh" -> "refresh";
            case "takemoney" -> "take-money: " + argument;
            case "givemoney" -> "give-money: " + argument;
            case "sound" -> sound(argument, where, line);
            default -> {
                warn(where, line, "this action has no counterpart, left out");
                yield null;
            }
        };
    }

    private String sound(String argument, String where, String line) {
        String[] words = argument.split("\\s+", 2);
        String key = soundKeys.apply(words[0]);
        if (key == null) {
            warn(where, line, "unknown sound, left out");
            return null;
        }
        return "sound: " + key + (words.length > 1 ? " " + words[1] : "");
    }

    private String text(String legacy) {
        char section = LegacyComponentSerializer.SECTION_CHAR;
        char ampersand = LegacyComponentSerializer.AMPERSAND_CHAR;
        if (legacy.indexOf(ampersand) < 0 && legacy.indexOf(section) < 0) {
            return legacy;
        }
        return MiniMessage.miniMessage().serialize(LEGACY.deserialize(legacy.replace(section, ampersand)));
    }

    private static void copy(ConfigurationSection from, String key, Map<String, Object> to, String newKey) {
        if (from.contains(key)) {
            to.put(newKey, from.get(key));
        }
    }

    private void warn(String where, String key, String problem) {
        warnings++;
        logger.warn("DeluxeMenus import, {} ({}): {}.", where, key, problem);
    }

    // Menus listed in DeluxeMenus' config.yml, each either in its own file or written inline.
    private Map<String, Source> sources(Path deluxeMenus) {
        Map<String, Source> sources = new LinkedHashMap<>();
        Path folder = deluxeMenus.resolve("gui_menus");
        Path configFile = deluxeMenus.resolve("config.yml");
        ConfigurationSection listed = Files.exists(configFile)
                ? YamlConfiguration.loadConfiguration(configFile.toFile()).getConfigurationSection("gui_menus")
                : null;
        if (listed != null) {
            for (String name : listed.getKeys(false)) {
                ConfigurationSection section = listed.getConfigurationSection(name);
                if (section == null) {
                    continue;
                }
                String id = name.toLowerCase(Locale.ROOT);
                if (section.isString("file")) {
                    Path file = folder.resolve(section.getString("file"));
                    sources.put(id, new Source("gui_menus/" + section.getString("file"),
                            YamlConfiguration.loadConfiguration(file.toFile())));
                } else {
                    sources.put(id, new Source("config.yml, gui_menus." + name, section));
                }
            }
            return sources;
        }

        // No config.yml: take every file and name the menu after it.
        if (Files.isDirectory(folder)) {
            try (Stream<Path> files = Files.walk(folder)) {
                files.filter(path -> path.toString().endsWith(".yml")).sorted().forEach(file -> {
                    String name = file.getFileName().toString();
                    sources.put(name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT),
                            new Source("gui_menus/" + folder.relativize(file).toString().replace('\\', '/'),
                                    YamlConfiguration.loadConfiguration(file.toFile())));
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return sources;
    }

    private record Source(String name, ConfigurationSection menu) {
    }

    private record Requirement(List<String> conditions, List<String> deny) {
    }
}
