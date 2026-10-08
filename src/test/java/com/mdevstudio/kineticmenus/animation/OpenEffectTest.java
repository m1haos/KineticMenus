package com.mdevstudio.kineticmenus.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OpenEffectTest {

    private static final Grid GRID = new Grid(5);
    private static final Map<Integer, String> BUTTONS = Map.of(4, "logo", 20, "left", 22, "middle", 24, "right",
            40, "close");

    private static Map<Integer, String> background() {
        Map<Integer, String> background = new HashMap<>();
        for (int slot = 0; slot < GRID.size(); slot++) {
            if (!BUTTONS.containsKey(slot)) {
                background.put(slot, "pane");
            }
        }
        return background;
    }

    private static Scene<String> scene(String spark, String trail) {
        return new Scene<>(GRID, BUTTONS, background(), Map.of(), spark, trail);
    }

    private static Animation<String> animate(OpenEffect effect, int duration) {
        return effect.animate(scene("spark", "trail"), duration, null, new Random(1));
    }

    @ParameterizedTest
    @EnumSource(OpenEffect.class)
    void everyEffectEndsOnTheFinishedMenu(OpenEffect effect) {
        assertEquals(scene(null, null).full(), animate(effect, 16).frames().getLast());
    }

    @ParameterizedTest
    @EnumSource(value = OpenEffect.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void effectsLastTheRequestedNumberOfTicksAndSoundInsideThem(OpenEffect effect) {
        Animation<String> animation = animate(effect, 16);

        assertEquals(16, animation.frames().size());
        assertFalse(animation.sounds().isEmpty());
        assertTrue(animation.sounds().stream().allMatch(cue -> cue.tick() >= 0 && cue.tick() < 16));
    }

    @ParameterizedTest
    @EnumSource(OpenEffect.class)
    void framesNeverLeaveTheMenu(OpenEffect effect) {
        for (Map<Integer, String> frame : animate(effect, 16).frames()) {
            assertTrue(frame.keySet().stream().allMatch(slot -> slot >= 0 && slot < GRID.size()));
        }
    }

    @Test
    void cascadeShowsTheTopRowFirstAndTheBottomRowLast() {
        List<Map<Integer, String>> frames = OpenEffect.CASCADE.animate(scene(null, null), 9, null, new Random(1))
                .frames();

        assertTrue(frames.getFirst().keySet().stream().allMatch(slot -> GRID.row(slot) == 0));
        assertTrue(frames.get(frames.size() - 2).keySet().stream().noneMatch(slot -> GRID.row(slot) == 4));
    }

    @Test
    void sparksLeadTheRevealOneTickAhead() {
        List<Map<Integer, String>> frames = animate(OpenEffect.TYPEWRITER, 12).frames();

        assertTrue(frames.getFirst().containsValue("spark"));
        for (int tick = 0; tick < frames.size() - 1; tick++) {
            Map<Integer, String> next = frames.get(tick + 1);
            frames.get(tick).forEach((slot, value) -> {
                if (value.equals("spark")) {
                    assertFalse("spark".equals(next.get(slot)), "slot " + slot + " sparked twice");
                }
            });
        }
    }

    @Test
    void waveStartsInTheMiddle() {
        Map<Integer, String> first = OpenEffect.WAVE.animate(scene(null, null), 10, null, new Random(1))
                .frames().getFirst();

        assertTrue(first.containsKey(GRID.slot(2, 4)));
        assertFalse(first.containsKey(0));
    }

    @Test
    void revealedSlotsNeverDisappear() {
        List<Map<Integer, String>> frames = OpenEffect.SPIRAL.animate(scene(null, null), 15, null, new Random(1))
                .frames();

        for (int tick = 1; tick < frames.size(); tick++) {
            assertTrue(frames.get(tick).keySet().containsAll(frames.get(tick - 1).keySet()));
        }
    }

    @Test
    void ringsVisitEverySlotOnceStartingAtTheCorner() {
        List<Integer> rings = GRID.rings();

        assertEquals(GRID.size(), rings.size());
        assertEquals(GRID.size(), new HashSet<>(rings).size());
        assertEquals(0, rings.getFirst());
        assertEquals(GRID.slot(1, 8), rings.get(Grid.COLUMNS));
        assertEquals(2 * Grid.COLUMNS + 2 * (GRID.rows() - 2), GRID.border().size());
    }

    @Test
    void travellingButtonsMoveOverAStillBackground() {
        Set<String> allowed = new HashSet<>(BUTTONS.values());
        allowed.addAll(List.of("pane", "trail"));
        for (OpenEffect effect : List.of(OpenEffect.SCATTER, OpenEffect.FALL, OpenEffect.BURST)) {
            for (Map<Integer, String> frame : animate(effect, 16).frames()) {
                assertTrue(allowed.containsAll(frame.values()), effect + " drew " + frame.values());
                assertTrue(frame.keySet().containsAll(background().keySet()), effect + " left holes");
            }
        }
    }

    @Test
    void fallingButtonsComeFromAboveAndLandLeftToRight() {
        List<Map<Integer, String>> frames = animate(OpenEffect.FALL, 16).frames();

        assertFalse(frames.getFirst().containsValue("close"));
        int left = firstTick(frames, 20, "left");
        int right = firstTick(frames, 24, "right");
        assertTrue(left < right, "left landed at " + left + ", right at " + right);
    }

    @Test
    void shuffleKeepsTheBackgroundAndStopsColumnByColumn() {
        List<Map<Integer, String>> frames = animate(OpenEffect.SHUFFLE, 16).frames();

        for (Map<Integer, String> frame : frames) {
            assertEquals("pane", frame.get(0));
            assertTrue(BUTTONS.containsValue(frame.get(20)));
        }
        assertTrue(settledFrom(frames, 20, "left") < settledFrom(frames, 24, "right"));
    }

    @Test
    void slidePushesThePreviousMenuOut() {
        Map<Integer, String> previous = new HashMap<>();
        for (int slot = 0; slot < GRID.size(); slot++) {
            previous.put(slot, "old");
        }
        Scene<String> scene = new Scene<>(GRID, BUTTONS, background(), previous, null, null);

        List<Map<Integer, String>> frames = OpenEffect.SLIDE_LEFT.animate(scene, 10, null, new Random(1)).frames();

        Map<Integer, String> middle = frames.get(4);
        assertEquals("old", middle.get(0));
        assertFalse("old".equals(middle.get(8)));
        assertFalse(frames.getLast().containsValue("old"));
    }

    @Test
    void emptyMenuOrSingleTickIsShownAtOnce() {
        Scene<String> empty = new Scene<>(GRID, Map.of(), Map.of(), Map.of(), null, null);
        assertEquals(List.of(Map.of()), OpenEffect.SCATTER.animate(empty, 10, null, new Random(1)).frames());
        assertEquals(1, animate(OpenEffect.WAVE, 1).frames().size());
    }

    @ParameterizedTest
    @EnumSource(Easing.class)
    void easingStartsAtZeroEndsAtOneAndNeverGoesBack(Easing easing) {
        assertEquals(0, easing.apply(0), 1e-9);
        assertEquals(1, easing.apply(1), 1e-9);
        for (int step = 0; step < 20; step++) {
            assertTrue(easing.apply((step + 1) / 20.0) >= easing.apply(step / 20.0));
        }
    }

    private static int firstTick(List<Map<Integer, String>> frames, int slot, String value) {
        for (int tick = 0; tick < frames.size(); tick++) {
            if (value.equals(frames.get(tick).get(slot))) {
                return tick;
            }
        }
        return Integer.MAX_VALUE;
    }

    // Unlike firstTick, a spinning reel may show the right item by chance before it stops.
    private static int settledFrom(List<Map<Integer, String>> frames, int slot, String value) {
        int tick = frames.size();
        while (tick > 0 && value.equals(frames.get(tick - 1).get(slot))) {
            tick--;
        }
        return tick;
    }
}
