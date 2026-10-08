package com.mdevstudio.kineticmenus.animation;

import com.mdevstudio.kineticmenus.animation.ClickEffect.Look;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Background motion while the menu is open. It only draws on empty slots and is computed from the menu's age
 * alone, so it costs nothing to keep and looks the same however often the menu is redrawn.
 */
public enum AmbientEffect {

    NONE,
    /** Lights run around the border. */
    CHASE,
    /** Empty slots light up one by one at random. */
    TWINKLE,
    /** The border slowly glows and fades. */
    PULSE;

    private static final int TWINKLE_PERIOD = 24;
    private static final Look[] PULSE_CYCLE = {null, Look.TRAIL, Look.ACCENT, Look.ACCENT, Look.TRAIL, null};
    // Two lights on menus with a long border, so one lap does not take forever.
    private static final int SECOND_LIGHT_FROM = 16;

    /**
     * @param speed ticks per step
     */
    public Map<Integer, Look> frame(Grid grid, Set<Integer> free, long age, int speed) {
        long step = age / Math.max(1, speed);
        Map<Integer, Look> frame = new HashMap<>();
        switch (this) {
            case CHASE -> chase(frame, grid, free, step);
            case TWINKLE -> free.forEach(slot -> {
                long phase = (step + scramble(slot)) % TWINKLE_PERIOD;
                if (phase == 0) {
                    frame.put(slot, Look.ACCENT);
                } else if (phase == 1) {
                    frame.put(slot, Look.TRAIL);
                }
            });
            case PULSE -> {
                Look look = PULSE_CYCLE[(int) (step % PULSE_CYCLE.length)];
                if (look != null) {
                    grid.border().forEach(slot -> put(frame, free, slot, look));
                }
            }
            default -> {
                // NONE leaves the background as it is
            }
        }
        return frame;
    }

    private static void chase(Map<Integer, Look> frame, Grid grid, Set<Integer> free, long step) {
        List<Integer> border = grid.border();
        int size = border.size();
        int lights = size >= SECOND_LIGHT_FROM ? 2 : 1;
        for (int light = 0; light < lights; light++) {
            int head = (int) ((step + (long) light * size / lights) % size);
            put(frame, free, border.get(Math.floorMod(head - 2, size)), Look.TRAIL);
            put(frame, free, border.get(Math.floorMod(head - 1, size)), Look.TRAIL);
            put(frame, free, border.get(head), Look.ACCENT);
        }
    }

    private static void put(Map<Integer, Look> frame, Set<Integer> free, int slot, Look look) {
        if (free.contains(slot)) {
            frame.put(slot, look);
        }
    }

    // Spreads neighbouring slots over the period so that twinkles do not run in visible lines.
    private static int scramble(int slot) {
        int mixed = slot * 0x9E3779B1;
        return Math.floorMod(mixed ^ (mixed >>> 15), TWINKLE_PERIOD);
    }
}
