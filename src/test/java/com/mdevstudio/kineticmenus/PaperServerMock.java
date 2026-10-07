package com.mdevstudio.kineticmenus;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.InventoryHolder;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.inventory.ChestInventoryMock;
import org.mockbukkit.mockbukkit.inventory.InventoryMock;

/**
 * MockBukkit does not implement {@code Inventory#getHolder(boolean)}, which the plugin relies on
 * to avoid block state snapshots. Inventories created by the plugin get it here.
 */
// The unchecked warning comes from ServerMock#getBanList, inherited as is.
@SuppressWarnings("unchecked")
class PaperServerMock extends ServerMock {

    @Override
    public InventoryMock createInventory(InventoryHolder holder, int size, Component title) {
        return new ChestInventoryMock(holder, size) {
            @Override
            public InventoryHolder getHolder(boolean useSnapshot) {
                return getHolder();
            }
        };
    }
}
