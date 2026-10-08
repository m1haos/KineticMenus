package com.mdevstudio.kineticmenus.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;

/**
 * How a menu appears. Every effect turns the scene into frames, one per tick, and brings its own sounds;
 * the last frame is always the finished menu, so it never ends up half drawn.
 */
public enum OpenEffect {

    NONE(Easing.LINEAR),
    /** Row by row, top to bottom. */
    CASCADE(Easing.EASE_IN_OUT),
    /** Rings spreading from the middle of the menu. */
    WAVE(Easing.EASE_OUT),
    /** Along the border inwards, like a snail shell. */
    SPIRAL(Easing.EASE_IN_OUT),
    /** One slot after another in reading order, with a bell at the end of the line. */
    TYPEWRITER(Easing.LINEAR),
    /** Buttons start in random slots and travel to their own. */
    SCATTER(Easing.EASE_IN_OUT),
    /** Buttons spin through the menu's items and stop column by column, like a slot machine. */
    SHUFFLE(Easing.EASE_IN),
    /** Buttons drop in from above, column after column. */
    FALL(Easing.EASE_IN),
    /** Buttons fly out of the middle to their places. */
    BURST(Easing.EASE_OUT),
    /** The previous menu moves out to the left and this one follows from the right. */
    SLIDE_LEFT(Easing.EASE_IN_OUT),
    SLIDE_RIGHT(Easing.EASE_IN_OUT),
    SLIDE_UP(Easing.EASE_IN_OUT),
    SLIDE_DOWN(Easing.EASE_IN_OUT);

    private static final double EPSILON = 1e-9;

    private final Easing easing;

    OpenEffect(Easing easing) {
        this.easing = easing;
    }

    /**
     * @param curve speed curve, or {@code null} for the one that suits the effect
     */
    public <T> Animation<T> animate(Scene<T> scene, int duration, Easing curve, RandomGenerator random) {
        Easing chosen = curve != null ? curve : easing;
        if (this == NONE || duration <= 1 || scene.items().isEmpty() && scene.previous().isEmpty()) {
            return Animation.still(scene.full());
        }
        Grid grid = scene.grid();
        return switch (this) {
            case CASCADE -> cascade(scene, duration, chosen);
            case WAVE -> new Animation<>(reveal(scene, duration, chosen, slot -> Math.hypot(
                    grid.row(slot) - grid.centerRow(), (grid.column(slot) - grid.centerColumn()) / 2)), List.of(
                    SoundCue.at(0, "block.amethyst_block.resonate", 0.5, 1.4),
                    SoundCue.at(duration - 1, "block.amethyst_block.chime", 1.0, 1.2)));
            case SPIRAL -> spiral(scene, duration, chosen);
            case TYPEWRITER -> typewriter(scene, duration, chosen);
            case SCATTER -> scatter(scene, duration, chosen, random);
            case SHUFFLE -> shuffle(scene, duration, chosen, random);
            case FALL -> fall(scene, duration, chosen);
            case BURST -> burst(scene, duration, chosen);
            case SLIDE_LEFT -> slide(scene, duration, chosen, 0, -1);
            case SLIDE_RIGHT -> slide(scene, duration, chosen, 0, 1);
            case SLIDE_UP -> slide(scene, duration, chosen, -1, 0);
            case SLIDE_DOWN -> slide(scene, duration, chosen, 1, 0);
            case NONE -> throw new AssertionError("handled above");
        };
    }

    private static <T> Animation<T> cascade(Scene<T> scene, int duration, Easing curve) {
        Grid grid = scene.grid();
        List<SoundCue> sounds = new ArrayList<>();
        for (int row = 0; row < grid.rows(); row++) {
            double position = grid.rows() == 1 ? 0 : row / (double) (grid.rows() - 1);
            // Falling chimes: every row a little lower than the one above.
            sounds.add(SoundCue.at(firstTick(position, duration, curve), "block.note_block.chime", 0.35,
                    1.6 - 0.12 * row));
        }
        return new Animation<>(reveal(scene, duration, curve, grid::row), sounds);
    }

    private static <T> Animation<T> spiral(Scene<T> scene, int duration, Easing curve) {
        Map<Integer, Integer> order = spiralOrder(scene.grid());
        List<SoundCue> sounds = new ArrayList<>();
        for (int tick = 0; tick < duration; tick += 3) {
            sounds.add(SoundCue.at(tick, "block.note_block.bell", 0.3, 0.8 + 0.8 * tick / duration));
        }
        return new Animation<>(reveal(scene, duration, curve, order::get), sounds);
    }

    private static <T> Animation<T> typewriter(Scene<T> scene, int duration, Easing curve) {
        List<SoundCue> sounds = new ArrayList<>();
        for (int tick = 0; tick < duration - 1; tick += 2) {
            sounds.add(SoundCue.at(tick, "block.note_block.hat", 0.4, tick % 4 == 0 ? 1.6 : 1.9));
        }
        sounds.add(SoundCue.at(duration - 1, "block.note_block.bell", 0.6, 2.0));
        return new Animation<>(reveal(scene, duration, curve, slot -> slot), sounds);
    }

    private static <T> List<Map<Integer, T>> reveal(Scene<T> scene, int duration, Easing curve,
                                                    ToDoubleFunction<Integer> rank) {
        Map<Integer, T> full = scene.full();
        Map<Integer, Double> positions = positions(full, rank);

        List<Map<Integer, T>> frames = new ArrayList<>(duration);
        for (int tick = 0; tick < duration; tick++) {
            double shown = curve.apply(tick / (double) (duration - 1));
            double next = curve.apply((tick + 1) / (double) (duration - 1));
            Map<Integer, T> frame = new HashMap<>();
            for (Map.Entry<Integer, T> entry : full.entrySet()) {
                double position = positions.get(entry.getKey());
                if (position <= shown + EPSILON) {
                    frame.put(entry.getKey(), entry.getValue());
                } else if (scene.spark() != null && position <= next + EPSILON) {
                    frame.put(entry.getKey(), scene.spark());
                }
            }
            frames.add(frame);
        }
        return frames;
    }

    private static <T> Map<Integer, Double> positions(Map<Integer, T> full, ToDoubleFunction<Integer> rank) {
        double min = full.keySet().stream().mapToDouble(rank::applyAsDouble).min().orElse(0);
        double max = full.keySet().stream().mapToDouble(rank::applyAsDouble).max().orElse(0);
        Map<Integer, Double> positions = new HashMap<>();
        full.keySet().forEach(slot -> positions.put(slot,
                max == min ? 0 : (rank.applyAsDouble(slot) - min) / (max - min)));
        return positions;
    }

    private static int firstTick(double position, int duration, Easing curve) {
        for (int tick = 0; tick < duration; tick++) {
            if (curve.apply(tick / (double) (duration - 1)) >= position - EPSILON) {
                return tick;
            }
        }
        return duration - 1;
    }

    private static <T> Animation<T> scatter(Scene<T> scene, int duration, Easing curve, RandomGenerator random) {
        Grid grid = scene.grid();
        // Small random delays keep the items from moving as one block.
        int maxDelay = duration / 4;
        Map<Integer, Flight> flights = new HashMap<>();
        scene.items().keySet().forEach(slot -> {
            int start = random.nextInt(grid.size());
            flights.put(slot, new Flight(grid.row(start), grid.column(start), random.nextInt(maxDelay + 1)));
        });
        return new Animation<>(travel(scene, duration, curve, flights, duration - 1 - maxDelay), List.of(
                SoundCue.at(0, "entity.illusioner.mirror_move", 0.6, 1.3),
                SoundCue.at(duration - 1, "block.amethyst_cluster.place", 0.9, 1.2)));
    }

    private static <T> Animation<T> fall(Scene<T> scene, int duration, Easing curve) {
        Grid grid = scene.grid();
        int stagger = duration / 3;
        int length = duration - 1 - stagger;
        Map<Integer, Flight> flights = new HashMap<>();
        scene.items().keySet().forEach(slot -> flights.put(slot, new Flight(grid.row(slot) - grid.rows(),
                grid.column(slot), grid.column(slot) * stagger / (Grid.COLUMNS - 1))));

        List<SoundCue> sounds = new ArrayList<>();
        // A soft knock for every column as it lands.
        flights.values().stream().mapToInt(Flight::delay).distinct().sorted().forEach(delay ->
                sounds.add(SoundCue.at(delay + length, "block.chiseled_bookshelf.insert", 0.4, 0.9 + 0.05 * delay)));
        return new Animation<>(travel(scene, duration, curve, flights, length), sounds);
    }

    private static <T> Animation<T> burst(Scene<T> scene, int duration, Easing curve) {
        Grid grid = scene.grid();
        Map<Integer, Flight> flights = new HashMap<>();
        scene.items().keySet().forEach(slot -> flights.put(slot,
                new Flight(Math.round(grid.centerRow()), Math.round(grid.centerColumn()), 0)));
        return new Animation<>(travel(scene, duration, curve, flights, duration - 1), List.of(
                SoundCue.at(0, "entity.wind_charge.wind_burst", 0.4, 1.5),
                SoundCue.at(duration - 1, "block.amethyst_block.chime", 0.8, 1.4)));
    }

    // Moves every item from its own starting point to its slot over the background; each flight takes `length`
    // ticks after its delay.
    private static <T> List<Map<Integer, T>> travel(Scene<T> scene, int duration, Easing curve,
                                                   Map<Integer, Flight> flights, int length) {
        Grid grid = scene.grid();
        List<Map<Integer, T>> frames = new ArrayList<>(duration);
        Map<Integer, Integer> previous = Map.of();
        for (int tick = 0; tick < duration; tick++) {
            Map<Integer, Integer> positions = new HashMap<>();
            for (Map.Entry<Integer, Flight> entry : flights.entrySet()) {
                int target = entry.getKey();
                Flight flight = entry.getValue();
                double progress = length <= 0 ? 1 : curve.apply((tick - flight.delay()) / (double) length);
                int row = (int) Math.round(lerp(flight.row(), grid.row(target), progress));
                int column = (int) Math.round(lerp(flight.column(), grid.column(target), progress));
                if (grid.contains(row, column)) {
                    positions.put(target, grid.slot(row, column));
                }
            }

            Map<Integer, T> frame = new HashMap<>(scene.background());
            if (scene.trail() != null) {
                previous.forEach((target, slot) -> {
                    if (!slot.equals(positions.get(target))) {
                        frame.put(slot, scene.trail());
                    }
                });
            }
            positions.forEach((target, slot) -> frame.put(slot, scene.items().get(target)));
            frames.add(frame);
            previous = positions;
        }
        return frames;
    }

    private static <T> Animation<T> shuffle(Scene<T> scene, int duration, Easing curve, RandomGenerator random) {
        Grid grid = scene.grid();
        List<T> values = new ArrayList<>(scene.items().values());
        // The reels start stopping after the first third and the last one stops on the final frame.
        int start = duration / 3;
        int[] stops = new int[Grid.COLUMNS];
        for (int column = 0; column < Grid.COLUMNS; column++) {
            stops[column] = start + (int) Math.round((duration - 1 - start)
                    * curve.apply((column + 1) / (double) Grid.COLUMNS));
        }

        Map<Integer, T> spinning = new HashMap<>();
        List<Map<Integer, T>> frames = new ArrayList<>(duration);
        for (int tick = 0; tick < duration; tick++) {
            // Spinning slows down: a new item every tick at first, every third tick near the end.
            boolean turn = tick % (1 + 2 * tick / duration) == 0;
            Map<Integer, T> frame = new HashMap<>(scene.background());
            for (Map.Entry<Integer, T> entry : scene.items().entrySet()) {
                int slot = entry.getKey();
                if (tick >= stops[grid.column(slot)]) {
                    frame.put(slot, entry.getValue());
                } else {
                    if (turn || !spinning.containsKey(slot)) {
                        spinning.put(slot, values.get(random.nextInt(values.size())));
                    }
                    frame.put(slot, spinning.get(slot));
                }
            }
            frames.add(frame);
        }

        List<SoundCue> sounds = new ArrayList<>();
        sounds.add(SoundCue.at(0, "item.book.page_turn", 0.8, 1.3));
        scene.items().keySet().stream().map(slot -> stops[grid.column(slot)]).distinct().sorted().forEach(tick ->
                sounds.add(SoundCue.at(tick, "ui.button.click", 0.25, 1.2 + 0.6 * tick / duration)));
        sounds.add(SoundCue.at(duration - 1, "block.note_block.pling", 0.5, 1.6));
        return new Animation<>(frames, sounds);
    }

    private static <T> Animation<T> slide(Scene<T> scene, int duration, Easing curve, int rowStep, int columnStep) {
        Grid grid = scene.grid();
        int distance = columnStep != 0 ? Grid.COLUMNS : grid.rows();
        Map<Integer, T> incoming = scene.full();

        List<Map<Integer, T>> frames = new ArrayList<>(duration);
        for (int tick = 0; tick < duration; tick++) {
            int shift = (int) Math.round(curve.apply((tick + 1) / (double) duration) * distance);
            Map<Integer, T> frame = new HashMap<>();
            place(frame, grid, scene.previous(), rowStep * shift, columnStep * shift);
            place(frame, grid, incoming, rowStep * (shift - distance), columnStep * (shift - distance));
            frames.add(frame);
        }
        return new Animation<>(frames, List.of(SoundCue.at(0, "item.book.page_turn", 1.0, 1.0)));
    }

    private static <T> void place(Map<Integer, T> frame, Grid grid, Map<Integer, T> view, int rows, int columns) {
        view.forEach((slot, item) -> {
            int row = slot / Grid.COLUMNS + rows;
            int column = slot % Grid.COLUMNS + columns;
            if (grid.contains(row, column)) {
                frame.put(grid.slot(row, column), item);
            }
        });
    }

    static Map<Integer, Integer> spiralOrder(Grid grid) {
        Map<Integer, Integer> order = new HashMap<>();
        int index = 0;
        for (int slot : grid.rings()) {
            order.put(slot, index++);
        }
        return order;
    }

    private static double lerp(double from, double to, double progress) {
        return from + (to - from) * progress;
    }

    // Where an item starts, possibly outside the menu, and how many ticks it waits before moving.
    private record Flight(double row, double column, int delay) {
    }
}
