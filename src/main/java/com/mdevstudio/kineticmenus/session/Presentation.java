package com.mdevstudio.kineticmenus.session;

import com.mdevstudio.kineticmenus.animation.OpenEffect;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

/**
 * How one particular opening of a menu looks and sounds for its viewer.
 *
 * @param previous what the viewer saw just before, for slide transitions
 * @param motion whether anything may move: off for players who turned animations off and for Bedrock players
 */
record Presentation(OpenEffect effect, Map<Integer, ItemStack> previous, boolean motion, boolean sound) {
}
