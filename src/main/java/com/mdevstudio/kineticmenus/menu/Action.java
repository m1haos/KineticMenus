package com.mdevstudio.kineticmenus.menu;

import com.mdevstudio.kineticmenus.animation.OpenEffect;
import org.bukkit.NamespacedKey;

public sealed interface Action {

    record Message(String text) implements Action {
    }

    record ConsoleCommand(String command) implements Action {
    }

    record PlayerCommand(String command) implements Action {
    }

    record Sound(NamespacedKey key, float volume, float pitch) implements Action {
    }

    /**
     * @param effect open effect for this transition only, or {@code null} for the menu's own
     */
    record Open(String menu, OpenEffect effect) implements Action {
    }

    record Close() implements Action {
    }

    record Refresh() implements Action {
    }
}
