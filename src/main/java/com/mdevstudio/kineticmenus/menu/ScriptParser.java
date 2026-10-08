package com.mdevstudio.kineticmenus.menu;

import com.mdevstudio.kineticmenus.animation.OpenEffect;
import com.mdevstudio.kineticmenus.animation.SoundCue;
import com.mdevstudio.kineticmenus.menu.Condition.Operator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.NamespacedKey;
import org.slf4j.Logger;

/**
 * Reads the one-line statements of menu files: actions like {@code "open: shop slide-left"}, conditions like
 * {@code "placeholder: %player_level% >= 10"} and sounds like {@code "block.note_block.pling 0.6 1.5"}.
 * Mistakes are logged with the file and key, and the line is skipped.
 */
final class ScriptParser {

    static final String AUTO = "auto";
    static final String NONE = "none";

    // Two-character operators first, so that ">=" is not read as ">" followed by "=".
    private static final Pattern COMPARISON = Pattern.compile("^(.*?)\\s*(==|!=|>=|<=|>|<)\\s*(.*)$");

    private final Logger logger;

    ScriptParser(Logger logger) {
        this.logger = logger;
    }

    List<Action> actions(List<String> lines, String where) {
        List<Action> actions = new ArrayList<>();
        for (String line : lines) {
            Action action = action(line, where);
            if (action != null) {
                actions.add(action);
            }
        }
        return List.copyOf(actions);
    }

    List<Condition> conditions(List<String> lines, String where) {
        List<Condition> conditions = new ArrayList<>();
        for (String line : lines) {
            Condition condition = condition(line, where);
            if (condition != null) {
                conditions.add(condition);
            }
        }
        return List.copyOf(conditions);
    }

    // null stands for auto, an empty list for none.
    List<SoundCue> sounds(Object value, String where) {
        if (value == null) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        if (value instanceof List<?> list) {
            list.forEach(line -> lines.add(String.valueOf(line)));
        } else {
            lines.add(String.valueOf(value));
        }
        if (lines.size() == 1 && lines.getFirst().trim().equalsIgnoreCase(AUTO)) {
            return null;
        }
        if (lines.size() == 1 && lines.getFirst().trim().equalsIgnoreCase(NONE)) {
            return List.of();
        }

        List<SoundCue> sounds = new ArrayList<>();
        for (String line : lines) {
            String[] words = line.trim().split("\\s+");
            NamespacedKey key = soundKey(words[0], where);
            if (key != null) {
                sounds.add(new SoundCue((int) number(words, 3, 0, where), key, (float) number(words, 1, 1, where),
                        (float) number(words, 2, 1, where)));
            }
        }
        return List.copyOf(sounds);
    }

    NamespacedKey soundKey(String name, String where) {
        NamespacedKey key = NamespacedKey.fromString(name.trim().toLowerCase(Locale.ROOT));
        if (key == null) {
            logger.warn("{}: sound '{}' should look like block.note_block.pling or minecraft:ui.button.click.", where,
                    name);
        }
        return key;
    }

    <E extends Enum<E>> E constant(Class<E> type, String name, E fallback, String where) {
        try {
            return Enum.valueOf(type, name.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            logger.warn("{}: '{}' is not one of {}.", where, name,
                    List.of(type.getEnumConstants()).toString().toLowerCase(Locale.ROOT).replace('_', '-'));
            return fallback;
        }
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
                NamespacedKey key = soundKey(words[0], where);
                yield key != null ? new Action.Sound(key, (float) number(words, 1, 1, where),
                        (float) number(words, 2, 1, where)) : null;
            }
            case "open" -> new Action.Open(words[0].toLowerCase(Locale.ROOT), words.length > 1
                    ? constant(OpenEffect.class, words[1], null, where) : null);
            case "close" -> new Action.Close();
            case "refresh" -> new Action.Refresh();
            case "take-money", "give-money" -> {
                double amount = amount(words[0], where);
                if (amount <= 0) {
                    yield null;
                }
                yield type.equals("take-money") ? new Action.TakeMoney(amount) : new Action.GiveMoney(amount);
            }
            default -> {
                logger.warn("{}: unknown action '{}'. Known: message, console, player, sound, open, close, refresh, "
                        + "take-money, give-money.", where, type);
                yield null;
            }
        };
    }

    private Condition condition(String line, String where) {
        String trimmed = line.trim();
        boolean negated = trimmed.startsWith("!");
        if (negated) {
            trimmed = trimmed.substring(1).trim();
        }
        int colon = trimmed.indexOf(':');
        String type = (colon < 0 ? trimmed : trimmed.substring(0, colon)).trim().toLowerCase(Locale.ROOT);
        String argument = colon < 0 ? "" : trimmed.substring(colon + 1).trim();

        Condition condition = switch (type) {
            case "permission" -> {
                if (argument.isEmpty()) {
                    logger.warn("{}: '{}' needs a permission after the colon.", where, line);
                    yield null;
                }
                yield new Condition.Permission(argument);
            }
            case "money" -> {
                double amount = amount(argument, where);
                yield amount > 0 ? new Condition.Money(amount) : null;
            }
            case "placeholder" -> comparison(argument, where);
            default -> {
                logger.warn("{}: unknown condition '{}'. Known: permission, money, placeholder.", where, type);
                yield null;
            }
        };
        return condition != null && negated ? new Condition.Not(condition) : condition;
    }

    private Condition comparison(String argument, String where) {
        Matcher matcher = COMPARISON.matcher(argument);
        if (!matcher.matches()) {
            logger.warn("{}: '{}' should compare two values, like %player_level% >= 10.", where, argument);
            return null;
        }
        Operator operator = null;
        for (Operator candidate : Operator.values()) {
            if (candidate.symbol().equals(matcher.group(2))) {
                operator = candidate;
            }
        }
        return new Condition.Compare(matcher.group(1).trim(), operator, matcher.group(3).trim());
    }

    private double amount(String text, String where) {
        try {
            double amount = Double.parseDouble(text);
            if (amount > 0 && Double.isFinite(amount)) {
                return amount;
            }
        } catch (NumberFormatException e) {
            // reported below together with negative amounts
        }
        logger.warn("{}: '{}' should be a positive amount of money.", where, text);
        return 0;
    }

    private double number(String[] words, int index, double fallback, String where) {
        if (words.length <= index) {
            return fallback;
        }
        try {
            return Double.parseDouble(words[index]);
        } catch (NumberFormatException e) {
            logger.warn("{}: '{}' should be a number, using {}.", where, words[index], fallback);
            return fallback;
        }
    }
}
