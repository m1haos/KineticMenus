package com.mdevstudio.kineticmenus.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;

/**
 * How a menu appears. Every effect turns the final layout into frames, one per tick; the last frame is
 * always the layout itself, so a menu never ends up half drawn.
 */
public enum OpenEffect {

    NONE,
    /** Row by row, top to bottom. */
    CASCADE,
    /** Rings spreading from the middle of the menu. */
    WAVE,
    /** Along the border inwards, like a snail shell. */
    SPIRAL,
    /** One slot after another in reading order. */
    TYPEWRITER,
    /** Items start in random slots and travel to their own. */
    SCATTER,
    /** Slots flicker through random items of the menu before settling. */
    SHUFFLE;

    public <T> List<Map<Integer, T>> frames(Map<Integer, T> layout, Grid grid, int duration, RandomGenerator random) {
        if (this == NONE || layout.isEmpty() || duration <= 1) {
            return List.of(Map.copyOf(layout));
        }
        return switch (this) {
            case CASCADE -> reveal(layout, duration, grid::row);
            case WAVE -> reveal(layout, duration, slot -> Math.hypot(
                    grid.row(slot) - grid.centerRow(), (grid.column(slot) - grid.centerColumn()) / 2));
            case SPIRAL -> {
                Map<Integer, Integer> order = spiralOrder(grid);
                yield reveal(layout, duration, order::get);
            }
            case TYPEWRITER -> reveal(layout, duration, slot -> slot);
            case SCATTER -> scatter(layout, grid, duration, random);
            case SHUFFLE -> shuffle(layout, duration, random);
            case NONE -> throw new AssertionError("handled above");
        };
    }

    private static <T> List<Map<Integer, T>> reveal(Map<Integer, T> layout, int duration,
                                                    ToDoubleFunction<Integer> rank) {
        double max = layout.keySet().stream().mapToDouble(rank::applyAsDouble).max().orElse(0);
        double min = layout.keySet().stream().mapToDouble(rank::applyAsDouble).min().orElse(0);
        double spread = max - min;

        List<Map<Integer, T>> frames = new ArrayList<>(duration);
        for (int tick = 0; tick < duration; tick++) {
            Map<Integer, T> frame = new HashMap<>();
            for (Map.Entry<Integer, T> entry : layout.entrySet()) {
                double position = spread == 0 ? 0 : (rank.applyAsDouble(entry.getKey()) - min) / spread;
                double appearsAt = position * (duration - 1);
                if (appearsAt <= tick + 1e-9) {
                    frame.put(entry.getKey(), entry.getValue());
                }
            }
            frames.add(frame);
        }
        return frames;
    }

    private static <T> List<Map<Integer, T>> scatter(Map<Integer, T> layout, Grid grid, int duration,
                                                     RandomGenerator random) {
        Map<Integer, Integer> starts = new LinkedHashMap<>();
        layout.keySet().forEach(slot -> starts.put(slot, random.nextInt(grid.size())));

        List<Map<Integer, T>> frames = new ArrayList<>(duration);
        for (int tick = 0; tick < duration; tick++) {
            double progress = easeOut((tick + 1) / (double) duration);
            Map<Integer, T> frame = new HashMap<>();
            for (Map.Entry<Integer, Integer> entry : starts.entrySet()) {
                int target = entry.getKey();
                int start = entry.getValue();
                int row = (int) Math.round(lerp(grid.row(start), grid.row(target), progress));
                int column = (int) Math.round(lerp(grid.column(start), grid.column(target), progress));
                frame.put(grid.slot(row, column), layout.get(target));
            }
            frames.add(frame);
        }
        return frames;
    }

    private static <T> List<Map<Integer, T>> shuffle(Map<Integer, T> layout, int duration, RandomGenerator random) {
        List<T> values = new ArrayList<>(layout.values());
        List<Map<Integer, T>> frames = new ArrayList<>(duration);
        for (int tick = 0; tick < duration - 1; tick++) {
            Map<Integer, T> frame = new HashMap<>();
            layout.keySet().forEach(slot -> frame.put(slot, values.get(random.nextInt(values.size()))));
            frames.add(frame);
        }
        frames.add(Map.copyOf(layout));
        return frames;
    }

    static Map<Integer, Integer> spiralOrder(Grid grid) {
        Map<Integer, Integer> order = new HashMap<>();
        int top = 0;
        int bottom = grid.rows() - 1;
        int left = 0;
        int right = Grid.COLUMNS - 1;
        int index = 0;
        while (top <= bottom && left <= right) {
            for (int column = left; column <= right; column++) {
                order.putIfAbsent(grid.slot(top, column), index++);
            }
            for (int row = top + 1; row <= bottom; row++) {
                order.putIfAbsent(grid.slot(row, right), index++);
            }
            for (int column = right - 1; column >= left && bottom > top; column--) {
                order.putIfAbsent(grid.slot(bottom, column), index++);
            }
            for (int row = bottom - 1; row > top && right > left; row--) {
                order.putIfAbsent(grid.slot(row, left), index++);
            }
            top++;
            bottom--;
            left++;
            right--;
        }
        return order;
    }

    private static double lerp(double from, double to, double progress) {
        return from + (to - from) * progress;
    }

    // Fast start, gentle landing: items look like they slide into place rather than stop dead.
    private static double easeOut(double progress) {
        return 1 - Math.pow(1 - progress, 3);
    }
}
