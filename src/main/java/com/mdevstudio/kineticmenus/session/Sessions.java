package com.mdevstudio.kineticmenus.session;

import com.mdevstudio.kineticmenus.KineticMenus;
import com.mdevstudio.kineticmenus.animation.OpenEffect;
import com.mdevstudio.kineticmenus.config.Messages;
import com.mdevstudio.kineticmenus.menu.Menu;
import com.mdevstudio.kineticmenus.render.IconRenderer;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Open menus. Each one is animated by its viewer's own scheduler, which keeps the plugin working on Folia.
 */
public final class Sessions {

    public enum Result {
        OPENED,
        NOT_FOUND,
        DENIED
    }

    private final KineticMenus plugin;
    private final ActionRunner actions;
    private final Requirements requirements;
    private final NamespacedKey reducedMotionKey;
    private final NamespacedKey mutedKey;
    private final Map<UUID, MenuSession> open = new ConcurrentHashMap<>();

    public Sessions(KineticMenus plugin) {
        this.plugin = plugin;
        this.actions = new ActionRunner(plugin, this, plugin::renderer, plugin::wallet);
        this.requirements = new Requirements(plugin::renderer, plugin::wallet);
        this.reducedMotionKey = new NamespacedKey(plugin, "reduced_motion");
        this.mutedKey = new NamespacedKey(plugin, "muted");
    }

    /**
     * Opens a menu on the player's own thread.
     *
     * @param effect open effect for this time only, or {@code null} for the menu's own
     */
    public Result open(Player player, String menuId, OpenEffect effect) {
        return open(player, menuId, effect, Map.of());
    }

    Result open(Player player, String menuId, OpenEffect effect, Map<Integer, ItemStack> previous) {
        Menu menu = plugin.menus().get(menuId);
        if (menu == null) {
            return Result.NOT_FOUND;
        }
        if (!requirements.test(menu.openIf(), player)) {
            if (menu.denyActions().isEmpty()) {
                plugin.messages().send(player, Messages.NO_ACCESS);
            } else {
                actions.run(menu.denyActions(), player, null);
            }
            return Result.DENIED;
        }

        boolean motion = !reducedMotion(player)
                && !(plugin.settings().bedrockReducedMotion() && plugin.bedrockPlayers().contains(player));
        OpenEffect chosen = !motion ? OpenEffect.NONE : effect != null ? effect : menu.opening().effect();
        MenuSession session = new MenuSession(this, menu, player,
                new Presentation(chosen, previous, motion, soundsFor(player)));
        // Opening closes whatever the player had open; its close event removes the old session first.
        session.start();
        open.put(player.getUniqueId(), session);
        return Result.OPENED;
    }

    /**
     * Puts every open menu back up from the freshly loaded files, without animation, so that an admin editing a
     * menu sees the change in place. Menus that no longer exist are closed.
     */
    public void reopenAll() {
        for (MenuSession session : new ArrayList<>(open.values())) {
            Player viewer = session.viewer();
            viewer.getScheduler().run(plugin, task -> {
                if (open.get(viewer.getUniqueId()) != session) {
                    return;
                }
                if (open(viewer, session.menu().id(), OpenEffect.NONE, Map.of()) != Result.OPENED) {
                    viewer.closeInventory();
                }
            }, null);
        }
    }

    public void closeAll() {
        new ArrayList<>(open.values()).forEach(session -> {
            session.stop();
            Player viewer = session.viewer();
            // On Folia an inventory may only be closed from its owner's thread.
            if (plugin.getServer().isOwnedByCurrentRegion(viewer)) {
                viewer.closeInventory();
            } else {
                viewer.getScheduler().run(plugin, task -> viewer.closeInventory(), null);
            }
        });
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

    public boolean muted(Player player) {
        return player.getPersistentDataContainer().getOrDefault(mutedKey, PersistentDataType.BOOLEAN, false);
    }

    public void setMuted(Player player, boolean muted) {
        player.getPersistentDataContainer().set(mutedKey, PersistentDataType.BOOLEAN, muted);
    }

    boolean soundsFor(Player player) {
        return plugin.settings().sounds() && !muted(player);
    }

    void closed(MenuSession session) {
        session.stop();
        open.remove(session.viewer().getUniqueId(), session);
    }

    KineticMenus plugin() {
        return plugin;
    }

    IconRenderer renderer() {
        return plugin.renderer();
    }

    Messages messages() {
        return plugin.messages();
    }

    ActionRunner actions() {
        return actions;
    }

    Requirements requirements() {
        return requirements;
    }

    float volume() {
        return plugin.settings().soundVolume();
    }
}
