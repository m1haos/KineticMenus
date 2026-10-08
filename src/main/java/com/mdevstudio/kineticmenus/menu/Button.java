package com.mdevstudio.kineticmenus.menu;

import com.mdevstudio.kineticmenus.animation.ClickEffect;
import com.mdevstudio.kineticmenus.animation.SoundCue;
import java.util.List;
import java.util.Map;

/**
 * @param priority when several buttons share a slot, the lowest number among those whose {@code showIf} is met
 *                 is shown
 * @param sound sounds of the click, {@code null} for the effect's own, empty for silence
 * @param deny what happens when {@code clickIf} is not met or the player cannot pay
 */
public record Button(String id, List<Integer> slots, int priority, Icon icon, Icon pressed,
                     List<Condition> showIf, List<Condition> clickIf, ClickEffect effect, List<SoundCue> sound,
                     Map<ClickKind, List<Action>> actions, Reaction deny) {

    public enum ClickKind {
        ANY,
        LEFT,
        RIGHT,
        SHIFT_LEFT,
        SHIFT_RIGHT
    }

    /**
     * @param sound {@code null} for the effect's own sounds, empty for silence
     */
    public record Reaction(ClickEffect effect, List<SoundCue> sound, List<Action> actions) {
    }

    public List<Action> actionsFor(ClickKind kind) {
        return actions.getOrDefault(kind, actions.getOrDefault(ClickKind.ANY, List.of()));
    }
}
