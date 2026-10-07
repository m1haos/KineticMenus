package com.mdevstudio.kineticmenus.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Feedback on a click. Effects are short sequences of overlays on top of the menu; a missing slot in an
 * overlay means "show the menu as usual there".
 */
public enum ClickEffect {

    NONE,
    /** The button sinks in for a moment. */
    PRESS,
    /** Two quick blinks of enchantment glint. */
    FLASH,
    /** The button jerks left and right, for refusals. */
    SHAKE,
    /** A ring of colour spreads from the button over the empty slots around it. */
    RIPPLE;

    public enum Look {
        PRESSED,
        GLINT,
        HIDDEN,
        BUTTON,
        RIPPLE
    }

    /**
     * @param free slots the effect may draw on without covering other buttons
     */
    public List<Map<Integer, Look>> frames(Grid grid, int slot, Set<Integer> free) {
        return switch (this) {
            case NONE -> List.of();
            case PRESS -> List.of(Map.of(slot, Look.PRESSED), Map.of(slot, Look.PRESSED), Map.of(slot, Look.PRESSED));
            case FLASH -> List.of(Map.of(slot, Look.GLINT), Map.of(), Map.of(slot, Look.GLINT), Map.of());
            case SHAKE -> shake(grid, slot, free);
            case RIPPLE -> ripple(grid, slot, free);
        };
    }

    private static List<Map<Integer, Look>> shake(Grid grid, int slot, Set<Integer> free) {
        List<Map<Integer, Look>> frames = new ArrayList<>();
        int row = grid.row(slot);
        for (int offset : new int[] {-1, 1, -1, 1}) {
            int column = grid.column(slot) + offset;
            int target = grid.contains(row, column) ? grid.slot(row, column) : -1;
            if (target >= 0 && free.contains(target)) {
                frames.add(Map.of(slot, Look.HIDDEN, target, Look.BUTTON));
            } else {
                // Nowhere to move on this side: blink instead, so the rhythm of the shake stays the same.
                frames.add(Map.of(slot, Look.HIDDEN));
            }
        }
        return frames;
    }

    private static List<Map<Integer, Look>> ripple(Grid grid, int slot, Set<Integer> free) {
        List<Map<Integer, Look>> frames = new ArrayList<>();
        for (int radius = 1; radius <= 4; radius++) {
            Set<Integer> ring = ring(grid, slot, radius);
            ring.retainAll(free);
            Map<Integer, Look> frame = new HashMap<>();
            ring.forEach(target -> frame.put(target, Look.RIPPLE));
            frames.add(frame);
        }
        frames.add(Map.of());
        return frames;
    }

    static Set<Integer> ring(Grid grid, int center, int radius) {
        Set<Integer> ring = new HashSet<>();
        int centerRow = grid.row(center);
        int centerColumn = grid.column(center);
        for (int row = centerRow - radius; row <= centerRow + radius; row++) {
            for (int column = centerColumn - radius; column <= centerColumn + radius; column++) {
                boolean onEdge = Math.max(Math.abs(row - centerRow), Math.abs(column - centerColumn)) == radius;
                if (onEdge && grid.contains(row, column)) {
                    ring.add(grid.slot(row, column));
                }
            }
        }
        return ring;
    }
}
