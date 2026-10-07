package com.mdevstudio.kineticmenus;

import com.mdevstudio.kineticmenus.config.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class KineticMenusCommand {

    private KineticMenusCommand() {
    }

    static void register(Commands registrar, KineticMenus plugin) {
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
                .then(Commands.literal("animations")
                        .requires(source -> source.getSender().hasPermission(Permissions.ANIMATIONS))
                        .then(Commands.literal("on")
                                .executes(context -> animations(plugin, context.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(context -> animations(plugin, context.getSource(), false))))
                .then(Commands.literal("list")
                        .requires(source -> source.getSender().hasPermission(Permissions.LIST))
                        .executes(context -> list(plugin, context.getSource().getSender())))
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(Permissions.RELOAD))
                        .executes(context -> reload(plugin, context.getSource().getSender())));

        registrar.register(root.build(), "Animated menus", List.of("km"));
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
        if (!plugin.sessions().open(target, menu.toLowerCase(Locale.ROOT), null)) {
            plugin.messages().send(sender, Messages.MENU_NOT_FOUND, Placeholder.unparsed("menu", menu));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int animations(KineticMenus plugin, CommandSourceStack source, boolean on) {
        if (!(source.getSender() instanceof Player player)) {
            plugin.messages().send(source.getSender(), Messages.PLAYERS_ONLY);
            return 0;
        }
        plugin.sessions().setReducedMotion(player, !on);
        plugin.messages().send(player, on ? Messages.ANIMATIONS_ON : Messages.ANIMATIONS_OFF);
        return Command.SINGLE_SUCCESS;
    }

    private static int list(KineticMenus plugin, CommandSender sender) {
        String names = plugin.menus().keySet().stream().sorted().reduce((a, b) -> a + ", " + b).orElse("-");
        plugin.messages().send(sender, Messages.LIST, Placeholder.unparsed("menus", names),
                Placeholder.unparsed("count", String.valueOf(plugin.menus().size())));
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(KineticMenus plugin, CommandSender sender) {
        plugin.reload();
        plugin.messages().send(sender, Messages.RELOADED,
                Placeholder.unparsed("count", String.valueOf(plugin.menus().size())));
        return Command.SINGLE_SUCCESS;
    }
}
