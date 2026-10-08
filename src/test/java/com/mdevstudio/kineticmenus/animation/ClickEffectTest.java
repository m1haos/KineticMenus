package com.mdevstudio.kineticmenus.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mdevstudio.kineticmenus.animation.ClickEffect.Look;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ClickEffectTest {

    private static final Grid GRID = new Grid(3);
    private static final int MIDDLE = GRID.slot(1, 4);
    private static final Set<Integer> ALL_FREE = IntStream.range(0, GRID.size()).boxed()
            .filter(slot -> slot != MIDDLE).collect(Collectors.toSet());

    private static Animation<Look> animate(ClickEffect effect, int slot, Set<Integer> free) {
        return effect.animate(GRID, slot, free, new Random(4));
    }

    @ParameterizedTest
    @EnumSource(ClickEffect.class)
    void effectsOnlyDrawOnTheButtonAndFreeSlots(ClickEffect effect) {
        Set<Integer> free = Set.of(GRID.slot(1, 3), GRID.slot(0, 0), GRID.slot(2, 5));

        for (Map<Integer, Look> frame : animate(effect, MIDDLE, free).frames()) {
            frame.keySet().forEach(slot -> assertTrue(slot == MIDDLE || free.contains(slot), effect + " drew " + slot));
        }
    }

    @ParameterizedTest
    @EnumSource(value = ClickEffect.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void everyEffectHasItsOwnSound(ClickEffect effect) {
        Animation<Look> animation = animate(effect, MIDDLE, ALL_FREE);

        assertFalse(animation.sounds().isEmpty());
        assertTrue(animation.sounds().stream().allMatch(cue -> cue.tick() <= animation.frames().size()));
    }

    @Test
    void rippleFrontIsFollowedByAFadingTrail() {
        List<Map<Integer, Look>> frames = animate(ClickEffect.RIPPLE, MIDDLE, ALL_FREE).frames();

        assertEquals(8, frames.getFirst().size());
        assertEquals(Look.TRAIL, frames.get(1).get(MIDDLE - 1));
        assertEquals(Look.ACCENT, frames.get(1).get(MIDDLE - 2));
        assertTrue(frames.getLast().values().stream().allMatch(look -> look == Look.TRAIL));
    }

    @Test
    void shakeMovesTheButtonSidewaysAndHidesItsSlot() {
        Map<Integer, Look> first = animate(ClickEffect.SHAKE, MIDDLE, ALL_FREE).frames().getFirst();

        assertEquals(Look.HIDDEN, first.get(MIDDLE));
        assertEquals(Look.BUTTON, first.get(MIDDLE - 1));
    }

    @Test
    void shakeAtTheEdgeKeepsItsRhythmWithoutLeavingTheMenu() {
        List<Map<Integer, Look>> frames = animate(ClickEffect.SHAKE, 0, Set.of(1)).frames();

        assertEquals(6, frames.size());
        assertEquals(Map.of(0, Look.HIDDEN), frames.getFirst());
    }

    @Test
    void sparkleKeepsTheButtonGlowingWhileSparksFade() {
        List<Map<Integer, Look>> frames = animate(ClickEffect.SPARKLE, MIDDLE, ALL_FREE).frames();

        assertTrue(frames.stream().allMatch(frame -> frame.get(MIDDLE) == Look.GLINT));
        assertTrue(frames.getFirst().containsValue(Look.ACCENT));
        assertTrue(frames.get(1).containsValue(Look.TRAIL));
    }

    @Test
    void ringsAreSquaresAroundTheCenter() {
        assertEquals(8, ClickEffect.ring(GRID, MIDDLE, 1).size());
        assertEquals(3, ClickEffect.ring(GRID, 0, 1).size());
    }
}
