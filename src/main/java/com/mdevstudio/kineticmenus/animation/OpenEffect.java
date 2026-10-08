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

    NONE,
    /** Row by row, top to bottom, each row sweeping in from the left. */
    CASCADE,
    /** Rings spreading from the middle of the menu. */
    WAVE,
    /** Along the border inwards, like a snail shell. */
    SPIRAL,
    /** One slot after another in reading order, with a bell at the end of the line. */
    TYPEWRITER,
    /** Buttons start in random slots and travel to their own. */
    SCATTER,
    /** Buttons spin through the menu's items and stop column by column, like a slot machine. */
    SHUFFLE,
    /** Buttons drop in from above, column after column. */
    FALL,
    /** Buttons fly out of the middle to their places. */
    BURST,
    /** The previous menu moves out to the left and this one follows from the right. */
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SLIDE_UP,
    SLIDE_DOWN;

    private static final double EPSILON = 1e-9;
    // How far a row's right end lags behind its left end, in rows: enough for a slanted curtain, not a typewriter.
    private static final double CASCADE_SLANT = 0.08;
    // A button crossing a short way slowly would stand still between slots, so flights are never slower than this;
    // the animation then simply ends sooner.
    private static final int MAX_TICKS_PER_SLOT = 2;

    /**
     * @param curve speed curve, or {@code null} for an even speed. Curves look best on long movements: on a grid of
     *              nine columns a slow start or end turns into ticks where nothing moves.
     */
    public <T> Animation<T> animate(Scene<T> scene, int duration, Easing curve, RandomGenerator random) {
        Easing chosen = curve != null ? curve : Easing.LINEAR;
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
        double last = grid.rows() - 1 + CASCADE_SLANT * (Grid.COLUMNS - 1);
        List<SoundCue> sounds = new ArrayList<>();
        for (int row = 0; row < grid.rows(); row++) {
            // Falling chimes: every row a little lower than the one above.
            sounds.add(SoundCue.at(firstTick(row / last, duration, curve), "block.note_block.chime", 0.35,
                    1.6 - 0.12 * row));
        }
        return new Animation<>(reveal(scene, duration, curve,
                slot -> grid.row(slot) + CASCADE_SLANT * grid.column(slot)), sounds);
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
        // Random departures keep the items from moving as one block and keep something in motion every tick.
        int maxDelay = duration / 3;
        Map<Integer, Integer> delays = new HashMap<>();
        scene.items().keySet().forEach(slot -> delays.put(slot, random.nextInt(maxDelay + 1)));
        // The first button always sets off at once.
        int earliest = delays.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        Map<Integer, Flight> flights = new HashMap<>();
        scene.items().keySet().forEach(slot -> {
            int start = random.nextInt(grid.size());
            int delay = delays.get(slot) - earliest;
            flights.put(slot, flight(grid, slot, grid.row(start), grid.column(start), delay, duration - 1 - delay));
        });
        List<Map<Integer, T>> frames = travel(scene, duration, curve, flights);
        return new Animation<>(frames, List.of(
                SoundCue.at(0, "entity.illusioner.mirror_move", 0.6, 1.3),
                SoundCue.at(frames.size() - 1, "block.amethyst_cluster.place", 0.9, 1.2)));
    }

    private static <T> Animation<T> fall(Scene<T> scene, int duration, Easing curve) {
        Grid grid = scene.grid();
        int stagger = duration / 3;
        // Everything drops from just above the menu at the same speed, so lower buttons land later; the speed is
        // chosen so that the last button lands on the last tick.
        double slowest = scene.items().keySet().stream().mapToDouble(slot -> (grid.row(slot) + 1)
                / (double) (duration - 1 - grid.column(slot) * stagger / (Grid.COLUMNS - 1))).max().orElse(1);
        double ticksPerRow = 1 / slowest;
        Map<Integer, Flight> flights = new HashMap<>();
        scene.items().keySet().forEach(slot -> flights.put(slot, flight(grid, slot, -1, grid.column(slot),
                grid.column(slot) * stagger / (Grid.COLUMNS - 1),
                (int) Math.round((grid.row(slot) + 1) * ticksPerRow))));

        Map<Integer, Integer> landings = new HashMap<>();
        flights.forEach((slot, flight) -> landings.merge(grid.column(slot), flight.delay() + flight.length(),
                Math::max));
        List<SoundCue> sounds = new ArrayList<>();
        // A soft knock for every column as its last button lands.
        List<Map<Integer, T>> frames = travel(scene, duration, curve, flights);
        landings.forEach((column, tick) -> sounds.add(SoundCue.at(Math.min(tick, frames.size() - 1),
                "block.chiseled_bookshelf.insert", 0.4, 0.9 + 0.05 * column)));
        return new Animation<>(frames, sounds);
    }

    private static <T> Animation<T> burst(Scene<T> scene, int duration, Easing curve) {
        Grid grid = scene.grid();
        int centerRow = (int) Math.round(grid.centerRow());
        int centerColumn = (int) Math.round(grid.centerColumn());
        double farthest = scene.items().keySet().stream().mapToDouble(slot -> distance(grid, slot, centerRow,
                centerColumn)).max().orElse(1);
        // Buttons fly at one speed, so the near ones settle first and the far ones keep the motion going.
        Map<Integer, Flight> flights = new HashMap<>();
        scene.items().keySet().forEach(slot -> flights.put(slot, flight(grid, slot, centerRow, centerColumn, 0,
                (int) Math.round((duration - 1) * distance(grid, slot, centerRow, centerColumn)
                        / Math.max(1, farthest)))));
        List<Map<Integer, T>> frames = travel(scene, duration, curve, flights);
        return new Animation<>(frames, List.of(
                SoundCue.at(0, "entity.wind_charge.wind_burst", 0.4, 1.5),
                SoundCue.at(frames.size() - 1, "block.amethyst_block.chime", 0.8, 1.4)));
    }

    private static double distance(Grid grid, int slot, double row, double column) {
        return Math.max(Math.abs(grid.row(slot) - row), Math.abs(grid.column(slot) - column));
    }

    private static Flight flight(Grid grid, int target, double row, double column, int delay, int length) {
        int fastest = (int) Math.ceil(distance(grid, target, row, column) * MAX_TICKS_PER_SLOT);
        return new Flight(row, column, delay, Math.max(1, Math.min(length, fastest)));
    }

    // Moves every item from its own starting point to its slot over the background.
    private static <T> List<Map<Integer, T>> travel(Scene<T> scene, int duration, Easing curve,
                                                   Map<Integer, Flight> flights) {
        Grid grid = scene.grid();
        int last = Math.min(duration - 1, flights.values().stream()
                .mapToInt(flight -> flight.delay() + flight.length()).max().orElse(0));
        List<Map<Integer, T>> frames = new ArrayList<>(last + 1);
        Map<Integer, Integer> previous = Map.of();
        for (int tick = 0; tick <= last; tick++) {
            Map<Integer, Integer> positions = new HashMap<>();
            for (Map.Entry<Integer, Flight> entry : flights.entrySet()) {
                int target = entry.getKey();
                Flight flight = entry.getValue();
                // Counting from the first tick of the flight makes a button move as soon as it sets off.
                double progress = tick == last ? 1
                        : curve.apply((tick - flight.delay() + 1) / (double) (flight.length() + 1));
                int row = (int) Math.round(lerp(flight.row(), grid.row(target), progress));
                int column = (int) Math.round(lerp(flight.column(), grid.column(target), progress));
                if (grid.contains(row, column)) {
                    positions.put(target, grid.slot(row, column));
                }
            }

            Map<Integer, T> frame = new HashMap<>(scene.background());
            if (scene.trail() != null && tick < last) {
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
        // Only columns with buttons stop, so the pauses between stops stay short in sparse menus.
        int start = duration / 3;
        List<Integer> columns = scene.items().keySet().stream().map(grid::column).distinct().sorted().toList();
        int[] stops = new int[Grid.COLUMNS];
        for (int index = 0; index < columns.size(); index++) {
            stops[columns.get(index)] = start + (int) Math.round((duration - 1 - start)
                    * curve.apply((index + 1) / (double) columns.size()));
        }

        Map<Integer, T> spinning = new HashMap<>();
        List<Map<Integer, T>> frames = new ArrayList<>(duration);
        for (int tick = 0; tick < duration; tick++) {
            // Spinning slows down: a new item every tick at first, every other tick near the end.
            boolean turn = 2 * tick < duration || tick % 2 == 0;
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
        // One column or row per tick whatever the menu's ticks say: a chest has only nine columns, and a slower
        // slide could only stand still between them.
        int length = Math.max(2, distance);
        Map<Integer, T> incoming = scene.full();

        List<Map<Integer, T>> frames = new ArrayList<>(length);
        for (int tick = 0; tick < length; tick++) {
            int shift = (int) Math.round(curve.apply((tick + 1) / (double) length) * distance);
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

    // Where an item starts, possibly outside the menu, how many ticks it waits and how many it travels.
    private record Flight(double row, double column, int delay, int length) {
    }
}
