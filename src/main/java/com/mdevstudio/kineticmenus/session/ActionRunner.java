package com.mdevstudio.kineticmenus.session;

import com.mdevstudio.kineticmenus.menu.Action;
import com.mdevstudio.kineticmenus.render.IconRenderer;
import java.util.List;
import java.util.function.Supplier;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class ActionRunner {

    private final Plugin plugin;
    private final Sessions sessions;
    private final Supplier<IconRenderer> renderer;

    ActionRunner(Plugin plugin, Sessions sessions, Supplier<IconRenderer> renderer) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.renderer = renderer;
    }

    void run(List<Action> actions, MenuSession session) {
        Player viewer = session.viewer();
        for (Action action : actions) {
            switch (action) {
                case Action.Message message -> viewer.sendMessage(renderer.get().text(message.text(), viewer, 0));
                case Action.ConsoleCommand command -> plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(), fill(command.command(), viewer));
                case Action.PlayerCommand command -> viewer.performCommand(fill(command.command(), viewer));
                case Action.Sound sound -> viewer.playSound(
                        Sound.sound(sound.key(), Sound.Source.MASTER, sound.volume(), sound.pitch()));
                // Opening or closing an inventory from inside its own click event is not allowed, so wait a tick.
                case Action.Open open -> later(() -> sessions.open(viewer, open.menu(), open.effect()));
                case Action.Close close -> later(viewer::closeInventory);
                case Action.Refresh refresh -> session.refresh();
            }
        }
    }

    private void later(Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    private static String fill(String command, Player viewer) {
        String filled = command.replace("{player}", viewer.getName());
        return filled.startsWith("/") ? filled.substring(1) : filled;
    }
}
