package com.mdevstudio.kineticmenus.menu;

import com.mdevstudio.kineticmenus.animation.Grid;
import com.mdevstudio.kineticmenus.animation.OpenEffect;
import java.util.List;
import org.bukkit.NamespacedKey;

/**
 * @param openSound played when the menu starts to appear, or {@code null}
 * @param filler icon for slots without a button, or {@code null} to leave them empty
 * @param ripple icon a ripple paints over empty slots
 */
public record Menu(String id, String title, Grid grid, OpenEffect openEffect, int openTicks, NamespacedKey openSound,
                   Icon filler, Icon ripple, List<Button> buttons, int refreshTicks) {

    public Button buttonAt(int slot) {
        for (Button button : buttons) {
            if (button.slots().contains(slot)) {
                return button;
            }
        }
        return null;
    }
}
