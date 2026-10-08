package com.mdevstudio.kineticmenus.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Only created when Vault is installed, so its classes are never loaded without it. The economy is looked up on
 * every call: economy plugins often register it after the server has started.
 */
public final class VaultWallet implements Wallet {

    private final Server server;

    public VaultWallet(Server server) {
        this.server = server;
    }

    @Override
    public boolean has(Player player, double amount) {
        Economy economy = economy();
        return economy != null && economy.has(player, amount);
    }

    @Override
    public boolean take(Player player, double amount) {
        Economy economy = economy();
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public void give(Player player, double amount) {
        Economy economy = economy();
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }

    private Economy economy() {
        RegisteredServiceProvider<Economy> registration = server.getServicesManager().getRegistration(Economy.class);
        return registration != null ? registration.getProvider() : null;
    }
}
