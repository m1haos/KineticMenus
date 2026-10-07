package com.mdevstudio.kineticmenus.menu;

import com.mdevstudio.kineticmenus.animation.ClickEffect;
import java.util.List;
import java.util.Map;

/**
 * @param permission needed to use the button, or {@code null} for everyone
 */
public record Button(String id, List<Integer> slots, Icon icon, Icon pressed, ClickEffect effect,
                     Map<ClickKind, List<Action>> actions, String permission, List<Action> denyActions,
                     ClickEffect denyEffect) {

    public enum ClickKind {
        ANY,
        LEFT,
        RIGHT,
        SHIFT_LEFT,
        SHIFT_RIGHT
    }

    public List<Action> actionsFor(ClickKind kind) {
        return actions.getOrDefault(kind, actions.getOrDefault(ClickKind.ANY, List.of()));
    }
}
