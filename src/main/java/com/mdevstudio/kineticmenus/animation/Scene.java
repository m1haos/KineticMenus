package com.mdevstudio.kineticmenus.animation;

import java.util.HashMap;
import java.util.Map;

/**
 * Everything an opening effect works with.
 *
 * @param items buttons, the part of the menu that moves
 * @param background filler under the buttons
 * @param previous what the player saw before this menu, used by slides; empty when there was nothing
 * @param spark drawn just ahead of items that are about to appear, or {@code null}
 * @param trail left for a tick where a moving item has just been, or {@code null}
 */
public record Scene<T>(Grid grid, Map<Integer, T> items, Map<Integer, T> background, Map<Integer, T> previous,
                       T spark, T trail) {

    public Map<Integer, T> full() {
        Map<Integer, T> full = new HashMap<>(background);
        full.putAll(items);
        return full;
    }
}
