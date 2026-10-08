package com.mdevstudio.kineticmenus;

import com.mdevstudio.kineticmenus.config.Messages;
import com.mdevstudio.kineticmenus.menu.Menu;
import com.mdevstudio.kineticmenus.migrate.DeluxeMenusImport;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class KineticMenusCommand {

    private KineticMenusCommand() {
    }

    /**
     * Registers /kmenu and the commands of every loaded menu.
     *
     * @return menu commands that were registered
     */
    static Set<String> register(Commands registrar, KineticMenus plugin) {
        var root = Commands.literal("kmenu")
                .then(Commands.literal("open")
                        .requires(source -> source.getSender().hasPermission(Permissions.OPEN))
                        .then(menuArgument(plugin)
                                .executes(context -> openSelf(plugin, context.getSource(),
                                        StringArgumentType.getString(context, "menu")))
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .requires(source -> source.getSender().hasPermission(Permissions.OPEN_OTHERS))
                                        .executes(context -> openFor(plugin, context.getSource().getSender(),
                                                StringArgumentType.getString(context, "menu"),
                                                context.getArgument("player", PlayerSelectorArgumentResolver.class)
                                                        .resolve(context.getSource()).getFirst())))))
                .then(toggle(plugin, "animations", Permissions.ANIMATIONS, (player, on) -> {
                    plugin.sessions().setReducedMotion(player, !on);
                    plugin.messages().send(player, on ? Messages.ANIMATIONS_ON : Messages.ANIMATIONS_OFF);
                }))
                .then(toggle(plugin, "sounds", Permissions.SOUNDS, (player, on) -> {
                    plugin.sessions().setMuted(player, !on);
                    plugin.messages().send(player, on ? Messages.SOUNDS_ON : Messages.SOUNDS_OFF);
                }))
                .then(Commands.literal("list")
                        .requires(source -> source.getSender().hasPermission(Permissions.LIST))
                        .executes(context -> list(plugin, context.getSource().getSender())))
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(Permissions.RELOAD))
                        .executes(context -> reload(plugin, context.getSource().getSender())))
                .then(Commands.literal("import")
                        .requires(source -> source.getSender().hasPermission(Permissions.IMPORT))
                        .then(Commands.literal("deluxemenus")
                                .executes(context -> importDeluxeMenus(plugin, context.getSource().getSender()))));
        registrar.register(root.build(), "Animated menus", List.of("km"));

        Set<String> registered = new HashSet<>();
        for (Menu menu : plugin.menus().values()) {
            for (String command : menu.commands()) {
                if (!registered.add(command)) {
                    plugin.getSLF4JLogger().warn("Menu '{}': command /{} already opens another menu, skipped.",
                            menu.id(), command);
                    continue;
                }
                registrar.register(Commands.literal(command)
                        .executes(context -> openSelf(plugin, context.getSource(), menu.id()))
                        .build(), "Opens the " + menu.id() + " menu");
            }
        }
        return Set.copyOf(registered);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toggle(KineticMenus plugin, String name,
                                                                    String permission,
                                                                    BiConsumer<Player, Boolean> apply) {
        return Commands.literal(name)
                .requires(source -> source.getSender().hasPermission(permission))
                .then(Commands.literal("on").executes(context -> toggle(plugin, context.getSource(), apply, true)))
                .then(Commands.literal("off").executes(context -> toggle(plugin, context.getSource(), apply, false)));
    }

    private static int toggle(KineticMenus plugin, CommandSourceStack source, BiConsumer<Player, Boolean> apply,
                              boolean on) {
        if (!(source.getSender() instanceof Player player)) {
            plugin.messages().send(source.getSender(), Messages.PLAYERS_ONLY);
            return 0;
        }
        apply.accept(player, on);
        return Command.SINGLE_SUCCESS;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> menuArgument(KineticMenus plugin) {
        return Commands.argument("menu", StringArgumentType.word())
                .suggests((context, builder) -> {
                    String typed = builder.getRemainingLowerCase();
                    plugin.menus().keySet().stream().filter(id -> id.startsWith(typed)).sorted()
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                });
    }

    private static int openSelf(KineticMenus plugin, CommandSourceStack source, String menu) {
        if (!(source.getSender() instanceof Player player)) {
            plugin.messages().send(source.getSender(), Messages.PLAYERS_ONLY);
            return 0;
        }
        return openFor(plugin, player, menu, player);
    }

    private static int openFor(KineticMenus plugin, CommandSender sender, String menu, Player target) {
        String id = menu.toLowerCase(Locale.ROOT);
        if (!plugin.menus().containsKey(id)) {
            plugin.messages().send(sender, Messages.MENU_NOT_FOUND, Placeholder.unparsed("menu", menu));
            return 0;
        }
        // On Folia the console and other players run commands on other threads than the target's.
        if (plugin.getServer().isOwnedByCurrentRegion(target)) {
            plugin.sessions().open(target, id, null);
        } else {
            target.getScheduler().run(plugin, task -> plugin.sessions().open(target, id, null), null);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int list(KineticMenus plugin, CommandSender sender) {
        String names = String.join(", ", plugin.menus().keySet().stream().sorted().toList());
        plugin.messages().send(sender, Messages.LIST, Placeholder.unparsed("menus", names.isEmpty() ? "-" : names),
                Placeholder.unparsed("count", String.valueOf(plugin.menus().size())));
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(KineticMenus plugin, CommandSender sender) {
        plugin.reload();
        plugin.messages().send(sender, Messages.RELOADED,
                Placeholder.unparsed("count", String.valueOf(plugin.menus().size())));
        List<String> waiting = plugin.unregisteredCommands();
        if (!waiting.isEmpty()) {
            plugin.messages().send(sender, Messages.COMMANDS_NEED_RESTART,
                    Placeholder.unparsed("commands", "/" + String.join(", /", waiting)));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int importDeluxeMenus(KineticMenus plugin, CommandSender sender) {
        Path source = plugin.getDataPath().resolveSibling("DeluxeMenus");
        if (!Files.isDirectory(source)) {
            plugin.messages().send(sender, Messages.IMPORT_NOT_FOUND);
            return 0;
        }
        DeluxeMenusImport.Report report = new DeluxeMenusImport(plugin.getSLF4JLogger(),
                KineticMenusCommand::soundKey).run(source, plugin.getDataPath().resolve("menus/imported"));
        plugin.reload();
        plugin.messages().send(sender, Messages.IMPORTED,
                Placeholder.unparsed("count", String.valueOf(report.imported().size())),
                Placeholder.unparsed("skipped", String.valueOf(report.skipped().size())),
                Placeholder.unparsed("warnings", String.valueOf(report.warnings())));
        return Command.SINGLE_SUCCESS;
    }

    // DeluxeMenus names sounds the Bukkit way, ENTITY_PLAYER_LEVELUP for entity.player.levelup.
    private static String soundKey(String name) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT).keyStream()
                .filter(key -> key.getKey().replace('.', '_').equalsIgnoreCase(name))
                .map(NamespacedKey::getKey)
                .findFirst()
                .orElse(null);
    }
}
