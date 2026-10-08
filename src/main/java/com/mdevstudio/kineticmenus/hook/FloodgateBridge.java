package com.mdevstudio.kineticmenus.hook;

import java.util.UUID;
import org.geysermc.floodgate.api.FloodgateApi;

// Kept apart so Floodgate classes are only loaded when the plugin is installed.
final class FloodgateBridge {

    private FloodgateBridge() {
    }

    static boolean isBedrock(UUID player) {
        return FloodgateApi.getInstance().isFloodgatePlayer(player);
    }
}
