package com.mdevstudio.kineticmenus.menu;

import com.mdevstudio.kineticmenus.animation.AmbientEffect;
import com.mdevstudio.kineticmenus.animation.Easing;
import com.mdevstudio.kineticmenus.animation.Grid;
import com.mdevstudio.kineticmenus.animation.OpenEffect;
import com.mdevstudio.kineticmenus.animation.SoundCue;
import java.util.Comparator;
import java.util.List;

/**
 * @param commands commands that open the menu, without the slash
 * @param openIf what the player needs to open the menu
 * @param denyActions run instead of opening when {@code openIf} is not met
 * @param filler icon for slots without a button, or {@code null} to leave them empty
 * @param accent bright icon effects draw with: ripple fronts, sparks, border lights
 * @param trail faint icon effects leave behind
 */
public record Menu(String id, String title, Grid grid, List<String> commands, List<Condition> openIf,
                   List<Action> denyActions, Opening opening, Ambient ambient, Icon filler, Icon accent, Icon trail,
                   List<Button> buttons, int refreshTicks) {

    /**
     * @param easing speed curve, or {@code null} for the one that suits the effect
     * @param sound {@code null} for the effect's own sounds, empty for silence
     */
    public record Opening(OpenEffect effect, int ticks, Easing easing, List<SoundCue> sound) {
    }

    /**
     * @param speed ticks per step of the effect
     */
    public record Ambient(AmbientEffect effect, int speed) {
    }

    public Menu {
        buttons = buttons.stream().sorted(Comparator.comparingInt(Button::priority)).toList();
    }
}
