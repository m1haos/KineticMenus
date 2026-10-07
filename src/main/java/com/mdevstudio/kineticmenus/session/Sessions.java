package com.mdevstudio.kineticmenus.session;

import com.mdevstudio.kineticmenus.KineticMenus;
import com.mdevstudio.kineticmenus.animation.OpenEffect;
import com.mdevstudio.kineticmenus.menu.Menu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Open menus and the single task that animates all of them.
 */
public final class Sessions {

    private final KineticMenus plugin;
    private final ActionRunner actions;
    private final NamespacedKey reducedMotionKey;
    private final Map<UUID, MenuSession> open = new HashMap<>();
    private BukkitTask ticker;

    public Sessions(KineticMenus plugin) {
        this.plugin = plugin;
        this.actions = new ActionRunner(plugin, this, plugin::renderer);
        this.reducedMotionKey = new NamespacedKey(plugin, "reduced_motion");
    }

    public void start() {
        ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    }

    public void stop() {
        if (ticker != null) {
            ticker.cancel();
        }
        closeAll();
    }

    /**
     * @param effect open effect for this time only, or {@code null} for the menu's own
     * @return {@code false} if there is no such menu
     */
    public boolean open(Player player, String menuId, OpenEffect effect) {
        Menu menu = plugin.menus().get(menuId);
        if (menu == null) {
            return false;
        }
        OpenEffect chosen = reducedMotion(player) ? OpenEffect.NONE : effect != null ? effect : menu.openEffect();
        MenuSession session = new MenuSession(menu, player, plugin.renderer(), actions, chosen);
        // Opening closes whatever the player had open; its close event removes the old session first.
        session.start();
        open.put(player.getUniqueId(), session);
        return true;
    }

    public void closeAll() {
        new ArrayList<>(open.values()).forEach(session -> session.viewer().closeInventory());
        open.clear();
    }

    public int openCount() {
        return open.size();
    }

    public boolean reducedMotion(Player player) {
        return player.getPersistentDataContainer().getOrDefault(reducedMotionKey, PersistentDataType.BOOLEAN, false);
    }

    public void setReducedMotion(Player player, boolean reduced) {
        player.getPersistentDataContainer().set(reducedMotionKey, PersistentDataType.BOOLEAN, reduced);
    }

    void closed(MenuSession session) {
        open.remove(session.viewer().getUniqueId(), session);
    }

    private void tick() {
        new ArrayList<>(open.values()).forEach(MenuSession::tick);
    }
}
