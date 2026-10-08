package com.mdevstudio.kineticmenus.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Feedback on a click: a short sequence of overlays on top of the menu and the sounds that go with it.
 * A missing slot in an overlay means "show the menu as usual there".
 */
public enum ClickEffect {

    NONE,
    /** The button sinks in and springs back. */
    PRESS,
    /** Two blinks of enchantment glint. */
    FLASH,
    /** The button jerks left and right, for refusals. */
    SHAKE,
    /** A ring of colour spreads from the button over the empty slots around it. */
    RIPPLE,
    /** The button glows and sparks fly around it, for purchases and rewards. */
    SPARKLE;

    private static final int RIPPLE_RADIUS = 8;
    private static final int SPARKLE_TICKS = 8;
    private static final int SPARKS_PER_BURST = 3;

    public enum Look {
        PRESSED,
        GLINT,
        HIDDEN,
        BUTTON,
        ACCENT,
        TRAIL
    }

    /**
     * @param free slots the effect may draw on without covering other buttons
     */
    public Animation<Look> animate(Grid grid, int slot, Set<Integer> free, RandomGenerator random) {
        return switch (this) {
            case NONE -> new Animation<>(List.of(), List.of());
            case PRESS -> new Animation<>(
                    List.of(Map.of(slot, Look.PRESSED), Map.of(slot, Look.PRESSED), Map.of(slot, Look.PRESSED)),
                    List.of(SoundCue.at(0, "ui.button.click", 0.4, 1.3), SoundCue.at(3, "ui.button.click", 0.2, 1.9)));
            case FLASH -> new Animation<>(
                    List.of(Map.of(slot, Look.GLINT), Map.of(slot, Look.GLINT), Map.of(), Map.of(),
                            Map.of(slot, Look.GLINT), Map.of(slot, Look.GLINT)),
                    List.of(SoundCue.at(0, "block.amethyst_block.hit", 0.8, 1.5),
                            SoundCue.at(4, "block.amethyst_block.hit", 0.8, 1.9)));
            case SHAKE -> shake(grid, slot, free);
            case RIPPLE -> ripple(grid, slot, free);
            case SPARKLE -> sparkle(grid, slot, free, random);
        };
    }

    private static Animation<Look> shake(Grid grid, int slot, Set<Integer> free) {
        List<Map<Integer, Look>> frames = new ArrayList<>();
        int row = grid.row(slot);
        // Each position is held for two ticks: at one tick per side the movement is too fast to follow.
        for (int offset : new int[] {-1, -1, 1, 1, -1, 1}) {
            int column = grid.column(slot) + offset;
            int target = grid.contains(row, column) ? grid.slot(row, column) : -1;
            if (target >= 0 && free.contains(target)) {
                frames.add(Map.of(slot, Look.HIDDEN, target, Look.BUTTON));
            } else {
                // Nowhere to move on this side: blink instead, so the rhythm of the shake stays the same.
                frames.add(Map.of(slot, Look.HIDDEN));
            }
        }
        return new Animation<>(frames, List.of(SoundCue.at(0, "block.note_block.bass", 0.7, 0.6),
                SoundCue.at(2, "block.note_block.bass", 0.7, 0.5)));
    }

    private static Animation<Look> ripple(Grid grid, int slot, Set<Integer> free) {
        List<Map<Integer, Look>> frames = new ArrayList<>();
        List<SoundCue> sounds = new ArrayList<>();
        Set<Integer> behind = Set.of();
        for (int radius = 1; radius <= RIPPLE_RADIUS; radius++) {
            Set<Integer> front = ring(grid, slot, radius);
            if (front.isEmpty() && behind.isEmpty()) {
                break;
            }
            Map<Integer, Look> frame = new HashMap<>();
            behind.stream().filter(free::contains).forEach(target -> frame.put(target, Look.TRAIL));
            front.stream().filter(free::contains).forEach(target -> frame.put(target, Look.ACCENT));
            frames.add(frame);
            if (!front.isEmpty()) {
                // An echo: each ring a little quieter and higher than the last.
                sounds.add(SoundCue.at(radius - 1, "block.amethyst_cluster.hit", 0.7 * Math.pow(0.65, radius - 1),
                        1.1 + 0.1 * radius));
            }
            behind = front;
        }
        return new Animation<>(frames, sounds);
    }

    private static Animation<Look> sparkle(Grid grid, int slot, Set<Integer> free, RandomGenerator random) {
        List<Integer> around = new ArrayList<>();
        for (int radius = 1; radius <= 2; radius++) {
            ring(grid, slot, radius).stream().filter(free::contains).sorted().forEach(around::add);
        }

        List<Map<Integer, Look>> frames = new ArrayList<>();
        Set<Integer> fading = Set.of();
        for (int tick = 0; tick < SPARKLE_TICKS; tick++) {
            Map<Integer, Look> frame = new HashMap<>();
            frame.put(slot, Look.GLINT);
            fading.forEach(target -> frame.put(target, Look.TRAIL));
            Set<Integer> sparks = new HashSet<>();
            if (tick % 2 == 0 && tick < SPARKLE_TICKS - 2) {
                for (int spark = 0; spark < SPARKS_PER_BURST && !around.isEmpty(); spark++) {
                    sparks.add(around.get(random.nextInt(around.size())));
                }
            }
            sparks.forEach(target -> frame.put(target, Look.ACCENT));
            frames.add(frame);
            fading = sparks;
        }
        return new Animation<>(frames, List.of(SoundCue.at(0, "entity.experience_orb.pickup", 0.35, 1.3),
                SoundCue.at(2, "entity.experience_orb.pickup", 0.35, 1.6),
                SoundCue.at(4, "entity.experience_orb.pickup", 0.35, 1.9),
                SoundCue.at(4, "block.amethyst_block.chime", 0.6, 1.5)));
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
