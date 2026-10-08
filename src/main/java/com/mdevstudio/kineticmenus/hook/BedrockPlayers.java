package com.mdevstudio.kineticmenus.hook;

import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Tells Bedrock players (joined through Geyser) apart. Their client redraws chest menus slowly, so animations
 * look like flicker there.
 */
public final class BedrockPlayers {

    private final boolean floodgate;

    public BedrockPlayers(boolean floodgate) {
        this.floodgate = floodgate;
    }

    public boolean contains(Player player) {
        UUID id = player.getUniqueId();
        // Without the Floodgate API: Floodgate gives Bedrock players ids whose upper half is zero.
        return floodgate ? FloodgateBridge.isBedrock(id) : id.getMostSignificantBits() == 0;
    }
}
