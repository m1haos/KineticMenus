package com.mdevstudio.kineticmenus.session;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class SessionListener implements Listener {

    private final Sessions sessions;

    public SessionListener(Sessions sessions) {
        this.sessions = sessions;
    }

    // Menu items are never real: every click is cancelled, which also stops shift-clicks, number keys
    // and double clicks from moving anything in or out.
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder(false) instanceof MenuSession session) {
            event.setCancelled(true);
            if (event.getClickedInventory() == top) {
                session.click(event.getSlot(), event.getClick());
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof MenuSession) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof MenuSession session) {
            sessions.closed(session);
        }
    }
}
