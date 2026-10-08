package com.mdevstudio.kineticmenus.hook;

import org.bukkit.entity.Player;

/**
 * Player money, as far as menus need it.
 */
public interface Wallet {

    /**
     * Used without Vault: nobody has money and every payment fails.
     */
    Wallet NONE = new Wallet() {
        @Override
        public boolean has(Player player, double amount) {
            return false;
        }

        @Override
        public boolean take(Player player, double amount) {
            return false;
        }

        @Override
        public void give(Player player, double amount) {
            // nothing to give money with
        }
    };

    boolean has(Player player, double amount);

    /**
     * @return {@code false} if the money could not be taken, for example because the balance is too low
     */
    boolean take(Player player, double amount);

    void give(Player player, double amount);
}
