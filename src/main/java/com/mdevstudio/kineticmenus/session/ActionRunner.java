package com.mdevstudio.kineticmenus.session;

import com.mdevstudio.kineticmenus.hook.Wallet;
import com.mdevstudio.kineticmenus.menu.Action;
import com.mdevstudio.kineticmenus.render.IconRenderer;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

final class ActionRunner {

    private final Plugin plugin;
    private final Sessions sessions;
    private final Supplier<IconRenderer> renderer;
    private final Supplier<Wallet> wallet;

    ActionRunner(Plugin plugin, Sessions sessions, Supplier<IconRenderer> renderer, Supplier<Wallet> wallet) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.renderer = renderer;
        this.wallet = wallet;
    }

    // false means the player could not pay. The total price is checked first, so a short balance stops the click
    // before any action runs. session is null for the deny actions of a menu that refused to open.
    boolean run(List<Action> actions, Player viewer, MenuSession session) {
        double price = actions.stream()
                .mapToDouble(action -> action instanceof Action.TakeMoney take ? take.amount() : 0)
                .sum();
        if (price > 0 && !wallet.get().has(viewer, price)) {
            return false;
        }

        Server server = plugin.getServer();
        for (Action action : actions) {
            switch (action) {
                case Action.Message message -> viewer.sendMessage(renderer.get().text(message.text(), viewer, 0));
                case Action.ConsoleCommand command -> {
                    String line = command(command.command(), viewer);
                    // On Folia the console may only run commands from the global region.
                    if (server.isGlobalTickThread()) {
                        server.dispatchCommand(server.getConsoleSender(), line);
                    } else {
                        server.getGlobalRegionScheduler().execute(plugin,
                                () -> server.dispatchCommand(server.getConsoleSender(), line));
                    }
                }
                case Action.PlayerCommand command -> viewer.performCommand(command(command.command(), viewer));
                case Action.Sound sound -> {
                    if (sessions.soundsFor(viewer)) {
                        viewer.playSound(Sound.sound(sound.key(), Sound.Source.UI,
                                sound.volume() * sessions.volume(), sound.pitch()));
                    }
                }
                // Opening or closing an inventory from inside its own click event is not allowed, so wait a tick.
                case Action.Open open -> {
                    Map<Integer, ItemStack> previous = session != null ? session.view() : Map.of();
                    later(viewer, () -> sessions.open(viewer, open.menu(), open.effect(), previous));
                }
                case Action.Close close -> later(viewer, viewer::closeInventory);
                case Action.Refresh refresh -> {
                    if (session != null) {
                        session.refresh();
                    }
                }
                case Action.TakeMoney take -> {
                    if (!wallet.get().take(viewer, take.amount())) {
                        return false;
                    }
                }
                case Action.GiveMoney give -> wallet.get().give(viewer, give.amount());
            }
        }
        return true;
    }

    private void later(Player viewer, Runnable task) {
        viewer.getScheduler().run(plugin, scheduled -> task.run(), null);
    }

    private String command(String command, Player viewer) {
        String filled = renderer.get().fill(command, viewer);
        return filled.startsWith("/") ? filled.substring(1) : filled;
    }
}
